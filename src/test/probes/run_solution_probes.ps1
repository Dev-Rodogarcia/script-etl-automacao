Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$resultsDir = Join-Path $root "src\test\results"
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"

$apiOut = Join-Path $resultsDir ("api_contracts_" + $timestamp + ".json")
$sqlOut = Join-Path $resultsDir ("sql_patterns_" + $timestamp + ".txt")
$algoOut = Join-Path $resultsDir ("algorithmic_fixes_" + $timestamp + ".json")
$contextOut = Join-Path $resultsDir ("implementation_context_" + $timestamp + ".md")

& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "probe_api_contracts.ps1") | Tee-Object -FilePath $apiOut
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "probe_sql_patterns.ps1") | Tee-Object -FilePath $sqlOut
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "probe_algorithmic_fixes.ps1") | Tee-Object -FilePath $algoOut
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "probe_implementation_context.ps1") | Tee-Object -FilePath $contextOut

[pscustomobject]@{
    api_contracts = $apiOut
    sql_patterns = $sqlOut
    algorithmic_fixes = $algoOut
    implementation_context = $contextOut
} | ConvertTo-Json -Compress
