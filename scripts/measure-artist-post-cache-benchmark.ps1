$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$markers = @(
    "ARTIST_POST_BASE_LIST_CACHE_BENCHMARK",
    "ARTIST_POST_BASE_DETAIL_CACHE_BENCHMARK",
    "ARTIST_POST_HOT_DATA_CACHE_BENCHMARK"
)

Push-Location $projectRoot
try {
    ./gradlew test --tests "*ArtistPostRedisCacheBenchmarkIntegrationTest" | Out-Null

    foreach ($marker in $markers) {
        $resultLine = Select-String -Path "build/test-results/test/TEST-*.xml" -Pattern $marker |
            Select-Object -First 1 -ExpandProperty Line

        if (-not $resultLine) {
            throw "Benchmark marker '$marker' not found in test results."
        }

        $resultLine
    }
} finally {
    Pop-Location
}
