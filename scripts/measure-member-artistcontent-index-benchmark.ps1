$ErrorActionPreference = "Stop"

$container = "springchatting-local-mysql"

function Invoke-MySqlQuery {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Query
    )

    $output = $Query | docker exec -i $container sh -lc "MYSQL_PWD=root_password mysql -uroot -N -s" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed.`n$query`n$output"
    }
    return ($output -join "`n")
}

function Parse-ExplainAnalyzeMs {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Output
    )

    $matches = [regex]::Matches($Output, "actual time=\d+(\.\d+)?\.\.(\d+(\.\d+)?)")
    if ($matches.Count -eq 0) {
        throw "Could not parse EXPLAIN ANALYZE output.`n$Output"
    }

    $maxMs = 0.0
    foreach ($match in $matches) {
        $endMs = [double]$match.Groups[2].Value
        if ($endMs -gt $maxMs) {
            $maxMs = $endMs
        }
    }

    return [math]::Round($maxMs, 3)
}

function Measure-QueryMs {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql,
        [int]$Repeat = 5
    )

    $times = New-Object System.Collections.Generic.List[double]

    Invoke-MySqlQuery -Query "USE springchatting_benchmark; EXPLAIN ANALYZE $Sql" | Out-Null

    for ($run = 0; $run -lt $Repeat; $run++) {
        $output = Invoke-MySqlQuery -Query "USE springchatting_benchmark; EXPLAIN ANALYZE $Sql"
        $times.Add((Parse-ExplainAnalyzeMs -Output $output))
    }

    $average = ($times | Measure-Object -Average).Average
    return [pscustomobject]@{
        AverageMs = [math]::Round($average, 3)
        Samples = ($times -join ", ")
    }
}

$setupSql = @"
CREATE DATABASE IF NOT EXISTS springchatting_benchmark;
USE springchatting_benchmark;

DROP TABLE IF EXISTS digits;
CREATE TABLE digits (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO digits (n) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

DROP TABLE IF EXISTS seq_100k;
CREATE TABLE seq_100k (
    n INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO seq_100k (n)
SELECT ones.n
     + 10 * tens.n
     + 100 * hundreds.n
     + 1000 * thousands.n
     + 10000 * tenThousands.n
     + 1 AS n
FROM digits ones
CROSS JOIN digits tens
CROSS JOIN digits hundreds
CROSS JOIN digits thousands
CROSS JOIN digits tenThousands;

DROP TABLE IF EXISTS fan_posts_before;
DROP TABLE IF EXISTS fan_posts_after;
CREATE TABLE fan_posts_before (
    id BIGINT NOT NULL PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    deleted_at DATETIME NULL
) ENGINE=InnoDB;
CREATE TABLE fan_posts_after LIKE fan_posts_before;
ALTER TABLE fan_posts_after ADD INDEX idx_fan_posts_artist_id_id (artist_id, id);

INSERT INTO fan_posts_before (id, artist_id, member_id, deleted_at)
SELECT n,
       MOD(n, 100) + 1,
       MOD(n, 1000) + 1,
       CASE WHEN MOD(n, 50) = 0 THEN '2026-01-01 00:00:00' ELSE NULL END
FROM seq_100k;
INSERT INTO fan_posts_after SELECT * FROM fan_posts_before;

DROP TABLE IF EXISTS artist_posts_before;
DROP TABLE IF EXISTS artist_posts_after;
CREATE TABLE artist_posts_before (
    id BIGINT NOT NULL PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    deleted_at DATETIME NULL
) ENGINE=InnoDB;
CREATE TABLE artist_posts_after LIKE artist_posts_before;
ALTER TABLE artist_posts_after ADD INDEX idx_artist_posts_artist_id_id (artist_id, id);

INSERT INTO artist_posts_before (id, artist_id, member_id, deleted_at)
SELECT n,
       MOD(n, 100) + 1,
       MOD(n, 1000) + 1,
       CASE WHEN MOD(n, 40) = 0 THEN '2026-01-01 00:00:00' ELSE NULL END
FROM seq_100k;
INSERT INTO artist_posts_after SELECT * FROM artist_posts_before;

DROP TABLE IF EXISTS comments_before;
DROP TABLE IF EXISTS comments_after;
CREATE TABLE comments_before (
    id BIGINT NOT NULL PRIMARY KEY,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    deleted_at DATETIME NULL
) ENGINE=InnoDB;
CREATE TABLE comments_after LIKE comments_before;
ALTER TABLE comments_after ADD INDEX idx_comments_target (target_type, target_id, id);
ALTER TABLE comments_after ADD INDEX idx_comments_parent (parent_id, id);

INSERT INTO comments_before (id, target_type, target_id, parent_id, deleted_at)
SELECT n,
       'ARTIST_POST',
       MOD(n, 100) + 1,
       NULL,
       CASE WHEN MOD(n, 60) = 0 THEN '2026-01-01 00:00:00' ELSE NULL END
FROM seq_100k;

INSERT INTO comments_before (id, target_type, target_id, parent_id, deleted_at)
SELECT n + 100000,
       'ARTIST_POST',
       MOD(n, 100) + 1,
       MOD(n, 1000) + 1,
       CASE WHEN MOD(n, 80) = 0 THEN '2026-01-01 00:00:00' ELSE NULL END
FROM seq_100k;
INSERT INTO comments_after SELECT * FROM comments_before;

DROP TABLE IF EXISTS follows_before;
DROP TABLE IF EXISTS follows_after;
CREATE TABLE follows_before (
    id BIGINT NOT NULL PRIMARY KEY,
    follower_member_id BIGINT NOT NULL,
    target_artist_member_id BIGINT NOT NULL
) ENGINE=InnoDB;
CREATE TABLE follows_after LIKE follows_before;
ALTER TABLE follows_after ADD INDEX idx_follows_follower_id_id (follower_member_id, id);

INSERT INTO follows_before (id, follower_member_id, target_artist_member_id)
SELECT n,
       MOD(n, 100) + 1,
       n
FROM seq_100k;
INSERT INTO follows_after SELECT * FROM follows_before;

ANALYZE TABLE fan_posts_before, fan_posts_after,
              artist_posts_before, artist_posts_after,
              comments_before, comments_after,
              follows_before, follows_after;
"@

Invoke-MySqlQuery -Query $setupSql | Out-Null

$queries = @(
    @{
        Name = "FanPost cursor"
        Before = "SELECT id, artist_id, member_id FROM fan_posts_before WHERE artist_id = 42 AND id < 90000 AND deleted_at IS NULL ORDER BY id DESC LIMIT 20"
        After = "SELECT id, artist_id, member_id FROM fan_posts_after WHERE artist_id = 42 AND id < 90000 AND deleted_at IS NULL ORDER BY id DESC LIMIT 20"
    },
    @{
        Name = "ArtistPost cursor"
        Before = "SELECT id, artist_id, member_id FROM artist_posts_before WHERE artist_id = 42 AND id < 90000 AND deleted_at IS NULL ORDER BY id DESC LIMIT 20"
        After = "SELECT id, artist_id, member_id FROM artist_posts_after WHERE artist_id = 42 AND id < 90000 AND deleted_at IS NULL ORDER BY id DESC LIMIT 20"
    },
    @{
        Name = "Comment root cursor"
        Before = "SELECT id, target_id, parent_id FROM comments_before WHERE target_type = 'ARTIST_POST' AND target_id = 42 AND parent_id IS NULL AND deleted_at IS NULL AND id < 95000 ORDER BY id DESC LIMIT 20"
        After = "SELECT id, target_id, parent_id FROM comments_after WHERE target_type = 'ARTIST_POST' AND target_id = 42 AND parent_id IS NULL AND deleted_at IS NULL AND id < 95000 ORDER BY id DESC LIMIT 20"
    },
    @{
        Name = "Comment replies"
        Before = "SELECT id, parent_id FROM comments_before WHERE parent_id = 500 AND deleted_at IS NULL ORDER BY id ASC LIMIT 20"
        After = "SELECT id, parent_id FROM comments_after WHERE parent_id = 500 AND deleted_at IS NULL ORDER BY id ASC LIMIT 20"
    },
    @{
        Name = "Follow list"
        Before = "SELECT id, follower_member_id, target_artist_member_id FROM follows_before WHERE follower_member_id = 42 ORDER BY id DESC LIMIT 20"
        After = "SELECT id, follower_member_id, target_artist_member_id FROM follows_after WHERE follower_member_id = 42 ORDER BY id DESC LIMIT 20"
    }
)

$rows = foreach ($query in $queries) {
    $before = Measure-QueryMs -Sql $query.Before
    $after = Measure-QueryMs -Sql $query.After
    $improvement = if ($before.AverageMs -eq 0) { 0 } else { [math]::Round((($before.AverageMs - $after.AverageMs) / $before.AverageMs) * 100, 1) }

    [pscustomobject]@{
        Query = $query.Name
        BeforeMs = $before.AverageMs
        AfterMs = $after.AverageMs
        ImprovementPercent = $improvement
        BeforeSamples = $before.Samples
        AfterSamples = $after.Samples
    }
}

"| Query | Before (ms) | After (ms) | Improvement |"
"| --- | ---: | ---: | ---: |"
foreach ($row in $rows) {
    "| $($row.Query) | $($row.BeforeMs) | $($row.AfterMs) | $($row.ImprovementPercent)% |"
}

""
"Raw samples"
foreach ($row in $rows) {
    "- $($row.Query): before=[$($row.BeforeSamples)] after=[$($row.AfterSamples)]"
}
