param(
    [int]$LookbackDays = 14
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

function Convert-DelimitedLines {
    param([string[]]$Lines)

    $filtered = @($Lines | Where-Object { $_ -and $_.Trim() -ne "" })
    if ($filtered.Count -lt 3) {
        return @()
    }

    $headers = @($filtered[0] -split "\|")
    $results = New-Object System.Collections.Generic.List[object]

    for ($i = 2; $i -lt $filtered.Count; $i++) {
        $line = $filtered[$i]
        if ($line -match "^-+\|") {
            continue
        }

        $parts = @($line -split "\|", $headers.Count)
        $item = [ordered]@{}
        for ($j = 0; $j -lt $headers.Count; $j++) {
            $name = $headers[$j].Trim()
            $value = if ($j -lt $parts.Count) { $parts[$j].Trim() } else { "" }
            $item[$name] = $value
        }
        $results.Add([pscustomobject]$item)
    }

    return [object[]]$results.ToArray()
}

function Get-DelimitedBlock {
    param(
        [string[]]$Lines,
        [string]$Marker
    )

    $start = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i] -eq $Marker) {
            $start = $i + 1
            break
        }
    }

    if ($start -lt 0) {
        return @()
    }

    $block = New-Object System.Collections.Generic.List[string]
    for ($i = $start; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        if ($line -like "###*") {
            break
        }
        $block.Add($line)
    }

    return @($block)
}

function Get-DaemonHistorySummary {
    param([string]$CsvPath)

    if (-not (Test-Path -LiteralPath $CsvPath)) {
        return [pscustomobject]@{
            file_found = $false
        }
    }

    $rows = Import-Csv -LiteralPath $CsvPath -Delimiter ';'
    $alerts = @($rows | Where-Object { $_.STATUS -eq "ALERT" })
    $errors = @($rows | Where-Object { $_.STATUS -eq "ERROR" })

    $currentAlertStreak = 0
    $maxAlertStreak = 0
    $currentNonSuccessStreak = 0
    $maxNonSuccessStreak = 0

    foreach ($row in $rows) {
        if ($row.STATUS -eq "ALERT") {
            $currentAlertStreak++
        } else {
            $currentAlertStreak = 0
        }
        if ($currentAlertStreak -gt $maxAlertStreak) {
            $maxAlertStreak = $currentAlertStreak
        }

        if ($row.STATUS -ne "SUCCESS") {
            $currentNonSuccessStreak++
        } else {
            $currentNonSuccessStreak = 0
        }
        if ($currentNonSuccessStreak -gt $maxNonSuccessStreak) {
            $maxNonSuccessStreak = $currentNonSuccessStreak
        }
    }

    return [pscustomobject]@{
        file_found = $true
        total_rows = $rows.Count
        alert_cycles = $alerts.Count
        error_cycles = $errors.Count
        max_consecutive_alert_cycles = $maxAlertStreak
        max_consecutive_non_success_cycles = $maxNonSuccessStreak
        sample_alerts = @($alerts | Select-Object -First 5 DATA_HORA_FIM, STATUS, DETALHE)
        sample_errors = @($errors | Select-Object -First 5 DATA_HORA_FIM, STATUS, DETALHE)
    }
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$cfg = Load-EnvFile -Path (Join-Path $root ".env")

$query = @"
SET NOCOUNT ON;

PRINT '###FKS';
SELECT
    fk.name AS fk_name,
    OBJECT_NAME(fk.parent_object_id) AS tabela_pai,
    OBJECT_NAME(fk.referenced_object_id) AS tabela_referenciada,
    fk.is_disabled,
    fk.is_not_trusted
FROM sys.foreign_keys fk
WHERE fk.parent_object_id IN (
    OBJECT_ID('dbo.coletas'),
    OBJECT_ID('dbo.fretes'),
    OBJECT_ID('dbo.manifestos'),
    OBJECT_ID('dbo.contas_a_pagar'),
    OBJECT_ID('dbo.faturas_graphql'),
    OBJECT_ID('dbo.faturas_por_cliente'),
    OBJECT_ID('dbo.localizacao_cargas'),
    OBJECT_ID('dbo.cotacoes'),
    OBJECT_ID('dbo.inventario'),
    OBJECT_ID('dbo.sinistros')
)
ORDER BY fk.name;

PRINT '###WATERMARKS';
SELECT
    entidade,
    CONVERT(varchar(19), watermark_confirmado, 120) AS watermark_confirmado
FROM dbo.sys_execution_watermark
ORDER BY entidade;

PRINT '###ORFAOS_MANIFESTOS';
SELECT
    COUNT(*) AS manifestos_orfaos,
    COUNT(DISTINCT m.pick_sequence_code) AS pick_sequence_codes_orfaos
FROM dbo.manifestos m
WHERE m.pick_sequence_code IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.coletas c
      WHERE c.sequence_code = m.pick_sequence_code
  );

PRINT '###ORFAOS_FRETES';
SELECT
    COUNT(*) AS fretes_orfaos,
    COUNT(DISTINCT f.accounting_credit_id) AS accounting_credit_ids_orfaos
FROM dbo.fretes f
WHERE f.accounting_credit_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.faturas_graphql fg
      WHERE fg.id = f.accounting_credit_id
  );

PRINT '###FRETES_DIA';
SELECT TOP ($LookbackDays)
    CONVERT(varchar(10), COALESCE(f.service_date, CONVERT(date, f.servico_em)), 120) AS service_day,
    COUNT(*) AS total_registros
FROM dbo.fretes f
WHERE COALESCE(f.service_date, CONVERT(date, f.servico_em)) IS NOT NULL
GROUP BY COALESCE(f.service_date, CONVERT(date, f.servico_em))
ORDER BY service_day DESC;

PRINT '###COLETAS_DIA';
SELECT TOP ($LookbackDays)
    CONVERT(varchar(10), request_date, 120) AS request_day,
    COUNT(*) AS total_registros
FROM dbo.coletas
WHERE request_date IS NOT NULL
GROUP BY request_date
ORDER BY request_day DESC;

PRINT '###AUDIT_STATUS_48H';
SELECT TOP (30)
    entidade,
    status_execucao,
    api_completa,
    api_total_unico,
    db_persistidos,
    CONVERT(varchar(19), finished_at, 120) AS finished_at
FROM dbo.sys_execution_audit
WHERE finished_at >= DATEADD(HOUR, -48, SYSDATETIME())
ORDER BY finished_at DESC;
"@

$sqlRaw = Invoke-SqlQuery -Config $cfg -Query $query
$sqlLines = @($sqlRaw | ForEach-Object { [string]$_ })

$foreignKeys = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###FKS"))
$watermarks = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###WATERMARKS"))
$orfaosManifestos = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###ORFAOS_MANIFESTOS"))
$orfaosFretes = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###ORFAOS_FRETES"))
$fretesPorDia = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###FRETES_DIA"))
$coletasPorDia = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###COLETAS_DIA"))
$auditStatus48h = @(Convert-DelimitedLines -Lines (Get-DelimitedBlock -Lines $sqlLines -Marker "###AUDIT_STATUS_48H"))

$result = [ordered]@{
    executed_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    lookback_days = $LookbackDays
    database = [ordered]@{
        foreign_keys = $foreignKeys
        watermarks = $watermarks
        orfaos_manifestos = $orfaosManifestos
        orfaos_fretes = $orfaosFretes
        fretes_por_dia = $fretesPorDia
        coletas_por_dia = $coletasPorDia
        audit_status_48h = $auditStatus48h
    }
    daemon = Get-DaemonHistorySummary -CsvPath (Join-Path $root "logs\daemon\historico\execucao_daemon_2026_04.csv")
}

$result | ConvertTo-Json -Depth 8
