param(
    [string]$DataInicio = ((Get-Date).AddDays(-1).ToString("yyyy-MM-dd")),
    [string]$DataFim = (Get-Date).ToString("yyyy-MM-dd"),
    [switch]$ExecutarValidacaoDetalhada
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Arquivo .env nao encontrado em $Path"
    }

    $cfg = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $key, $value = $line -split "=", 2
        $cfg[$key.Trim()] = $value.Trim()
    }

    return $cfg
}

function Invoke-CurlJson {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [string]$Body
    )

    $args = @("-sS", "-X", $Method, $Url)
    foreach ($name in $Headers.Keys) {
        $args += "-H"
        $args += ("{0}: {1}" -f $name, $Headers[$name])
    }
    $tempFile = $null
    try {
        if ($Body) {
            $tempFile = [System.IO.Path]::GetTempFileName()
            [System.IO.File]::WriteAllText($tempFile, $Body)
            $args += "--data-binary"
            $args += ("@" + $tempFile)
        }
        $args += "-w"
        $args += "`nHTTPSTATUS:%{http_code}"

        $raw = & curl.exe @args
    } finally {
        if ($tempFile -and (Test-Path -LiteralPath $tempFile)) {
            Remove-Item -LiteralPath $tempFile -Force
        }
    }
    $statusMatch = [regex]::Match($raw, "HTTPSTATUS:(\d{3})")
    $status = if ($statusMatch.Success) { $statusMatch.Groups[1].Value } else { "" }
    $bodyOnly = [regex]::Replace($raw, "\s*HTTPSTATUS:\d{3}\s*$", "")

    $json = $null
    try {
        $json = $bodyOnly | ConvertFrom-Json
    } catch {
        $json = $null
    }

    [pscustomobject]@{
        Status = $status
        Body = $bodyOnly
        Json = $json
    }
}

function Invoke-SqlQuery {
    param(
        [hashtable]$Config,
        [string]$Query
    )

    $database = ([regex]::Match($Config["DB_URL"], "databaseName=([^;]+)").Groups[1].Value)
    if (-not $database) {
        throw "Nao foi possivel extrair databaseName do DB_URL"
    }

    $server = "tcp:localhost,1433"
    $sqlcmd = "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\SQLCMD.EXE"

    & $sqlcmd `
        -S $server `
        -d $database `
        -U $Config["DB_USER"] `
        -P $Config["DB_PASSWORD"] `
        -C `
        -W `
        -s "|" `
        -Q $Query
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$cfg = Load-EnvFile -Path (Join-Path $root ".env")

$graphqlUrl = $cfg["API_BASE_URL"] + $cfg["API_GRAPHQL_ENDPOINT"]
$manifestosBase = $cfg["API_BASE_URL"] + "/api/analytics/reports/" + $cfg["TEMPLATE_MANIFESTOS"]
$range = "$DataInicio - $DataFim"

$graphqlHeaders = @{
    Authorization = "Bearer " + $cfg["API_GRAPHQL_TOKEN"]
    "Content-Type" = "application/json"
}
$dataExportHeaders = @{
    Authorization = "Bearer " + $cfg["API_DATAEXPORT_TOKEN"]
    "Content-Type" = "application/json"
    Accept = "application/json"
}

$graphqlBody = '{"query":"query HealthCheck { __typename }"}'
$dataExportPostBody = '{"search":{"manifests":{"service_date":"' + $range + '"}},"page":"1","per":"1","order_by":"sequence_code asc"}'
$dataExportGetUrl = $manifestosBase + "/data?search%5Bmanifests%5D%5Bservice_date%5D=" + [uri]::EscapeDataString($range) + "&page=1&per=1&order_by=sequence_code%20asc"

$graphql = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body $graphqlBody
$manifestosPost = Invoke-CurlJson -Method "POST" -Url ($manifestosBase + "/data") -Headers $dataExportHeaders -Body $dataExportPostBody
Start-Sleep -Seconds 3
$manifestosInfo = Invoke-CurlJson -Method "GET" -Url ($manifestosBase + "/info") -Headers @{ Authorization = "Bearer " + $cfg["API_DATAEXPORT_TOKEN"]; Accept = "application/json" } -Body $null
$manifestosGet = Invoke-CurlJson -Method "GET" -Url $dataExportGetUrl -Headers $dataExportHeaders -Body $null

$sqlSummary = Invoke-SqlQuery -Config $cfg -Query @"
SET NOCOUNT ON;
SELECT @@SERVERNAME AS server_name, DB_NAME() AS db_name,
       CASE WHEN OBJECT_ID('dbo.sys_execution_audit','U') IS NOT NULL THEN 1 ELSE 0 END AS sys_execution_audit_exists,
       CASE WHEN OBJECT_ID('dbo.sys_execution_watermark','U') IS NOT NULL THEN 1 ELSE 0 END AS sys_execution_watermark_exists,
       (SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id IN (OBJECT_ID('dbo.coletas'), OBJECT_ID('dbo.fretes'), OBJECT_ID('dbo.manifestos'), OBJECT_ID('dbo.contas_a_pagar'), OBJECT_ID('dbo.faturas_graphql'), OBJECT_ID('dbo.faturas_por_cliente'), OBJECT_ID('dbo.localizacao_cargas'), OBJECT_ID('dbo.cotacoes'), OBJECT_ID('dbo.inventario'), OBJECT_ID('dbo.sinistros'))) AS managed_foreign_keys,
       (SELECT COUNT(*) FROM dbo.log_extracoes WHERE timestamp_fim >= DATEADD(HOUR,-24,SYSDATETIME())) AS log_extracoes_24h;
"@

$graphqlTypeName = $null
if ($graphql.Json -and $graphql.Json.PSObject.Properties.Name -contains "data") {
    if ($graphql.Json.data -and $graphql.Json.data.PSObject.Properties.Name -contains "__typename") {
        $graphqlTypeName = $graphql.Json.data.__typename
    }
}

$summary = [ordered]@{
    executed_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    data_inicio = $DataInicio
    data_fim = $DataFim
    graphql = [ordered]@{
        http_status = $graphql.Status
        ok = (
            $graphql.Json -ne $null -and (
                ($graphql.Json.PSObject.Properties.Name -notcontains "errors") -or
                $null -eq $graphql.Json.errors
            )
        )
        typename = $graphqlTypeName
    }
    dataexport = [ordered]@{
        post_data_status = $manifestosPost.Status
        post_data_preview = $manifestosPost.Body.Substring(0, [Math]::Min(120, $manifestosPost.Body.Length))
        get_info_status = $manifestosInfo.Status
        get_info_ok = [bool]($manifestosInfo.Json -ne $null)
        get_data_status = $manifestosGet.Status
        get_data_json = [bool]($manifestosGet.Json -ne $null)
        get_data_preview = $manifestosGet.Body.Substring(0, [Math]::Min(120, $manifestosGet.Body.Length))
    }
    database = [ordered]@{
        sqlcmd_summary = $sqlSummary
    }
}

if ($ExecutarValidacaoDetalhada) {
    $mvnOutput = & mvn --% -q -DskipTests org.codehaus.mojo:exec-maven-plugin:3.5.1:java -Dexec.mainClass=br.com.extrator.bootstrap.Main -Dexec.args="--validar-api-banco-24h-detalhado --sem-faturas-graphql" 2>&1
    $summary["validacao_detalhada"] = [ordered]@{
        exit_code = $LASTEXITCODE
        tail = ($mvnOutput | Select-Object -Last 40)
    }
}

$summary | ConvertTo-Json -Depth 6
