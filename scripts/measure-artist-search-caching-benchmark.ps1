$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$marker = "ARTIST_SEARCH_CACHE_BENCHMARK"

Push-Location $projectRoot
try {
    ./gradlew test --tests "*ArtistSearchCachingBenchmarkIntegrationTest" | Out-Null

    $resultLine = Select-String -Path "build/test-results/test/TEST-*.xml" -Pattern $marker |
        Select-Object -First 1 -ExpandProperty Line

    if (-not $resultLine) {
        throw "Benchmark marker '$marker' not found in test results."
    }

    $resultLine
} finally {
    Pop-Location
}
