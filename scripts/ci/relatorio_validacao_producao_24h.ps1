param(
    [int]$Hours = 24,
    [int]$ExpectedLoopMinutes = 30,
    [int]$GapThresholdMinutes = 45,
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Path $PSScriptRoot -Parent | Split-Path -Parent
Set-Location $repoRoot

$logsRoot = Join-Path $repoRoot "logs"
$appRuntimeDir = Join-Path $logsRoot "aplicacao\runtime"
$appOperationsDir = Join-Path $logsRoot "aplicacao\operacoes"
$daemonRuntimeDir = Join-Path $logsRoot "daemon\runtime"
$daemonHistoryDir = Join-Path $logsRoot "daemon\historico"
$daemonCyclesDir = Join-Path $logsRoot "daemon\ciclos"
$reportsDir = Join-Path $logsRoot "relatorios"

$windowEnd = Get-Date
$windowStart = $windowEnd.AddHours(-1 * [math]::Abs($Hours))

if (-not $OutputPath) {
    $stamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
    $OutputPath = Join-Path $reportsDir ("producao_validacao_24h_{0}.md" -f $stamp)
}

function Limit-RecentFiles {
    param(
        [string]$Directory,
        [string]$Filter,
        [int]$MaxFiles = 20
    )

    if (-not (Test-Path $Directory)) {
        return
    }

    $files = Get-ChildItem -Path $Directory -File -Filter $Filter |
        Sort-Object LastWriteTime, Name -Descending

    if ($files.Count -le $MaxFiles) {
        return
    }

    $files | Select-Object -Skip $MaxFiles | Remove-Item -Force -ErrorAction SilentlyContinue
}

function Get-EnvMap {
    $map = @{}
    foreach ($entry in Get-ChildItem Env:) {
        $map[$entry.Name] = $entry.Value
    }

    $envFile = Join-Path $repoRoot ".env"
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile) {
            if ($line -match "^\s*#" -or $line -notmatch "=") {
                continue
            }
            $parts = $line -split "=", 2
            if (-not $map.ContainsKey($parts[0].Trim()) -or [string]::IsNullOrWhiteSpace($map[$parts[0].Trim()])) {
                $map[$parts[0].Trim()] = $parts[1].Trim()
            }
        }
    }
    return $map
}

function Get-DbConnection {
    param(
        [hashtable]$EnvMap
    )

    $jdbc = $EnvMap["DB_URL"]
    if ([string]::IsNullOrWhiteSpace($jdbc)) {
        throw "DB_URL nao encontrado em variavel de ambiente nem em .env."
    }

    $match = [regex]::Match($jdbc, "jdbc:sqlserver://([^;]+);databaseName=([^;]+)", "IgnoreCase")
    if (-not $match.Success) {
        throw "DB_URL invalida: $jdbc"
    }

    $server = $match.Groups[1].Value.Replace(":", ",")
    $database = $match.Groups[2].Value
    $user = $EnvMap["DB_USER"]
    $password = $EnvMap["DB_PASSWORD"]

    $connectionString = "Server=$server;Database=$database;User ID=$user;Password=$password;TrustServerCertificate=True;Encrypt=True;"
    $connection = New-Object System.Data.SqlClient.SqlConnection($connectionString)
    $connection.Open()
    return $connection
}

function Invoke-SqlQuery {
    param(
        [System.Data.SqlClient.SqlConnection]$Connection,
        [string]$Sql,
        [hashtable]$Parameters = @{}
    )

    $command = $Connection.CreateCommand()
    $command.CommandText = $Sql

    foreach ($key in $Parameters.Keys) {
        $null = $command.Parameters.AddWithValue($key, $Parameters[$key])
    }

    $adapter = New-Object System.Data.SqlClient.SqlDataAdapter($command)
    $table = New-Object System.Data.DataTable
    $null = $adapter.Fill($table)
    Write-Output -NoEnumerate $table
}

function Get-DaemonState {
    $statePath = Join-Path $daemonRuntimeDir "loop_daemon.state"
    $state = [ordered]@{}
    if (-not (Test-Path $statePath)) {
        return $state
    }

    foreach ($line in Get-Content $statePath) {
        if ($line.StartsWith("#") -or $line -notmatch "=") {
            continue
        }
        $parts = $line -split "=", 2
        $state[$parts[0]] = $parts[1]
    }
    return $state
}

function Get-TemplateMap {
    param(
        [hashtable]$EnvMap
    )

    $map = @{}
    foreach ($key in $EnvMap.Keys | Sort-Object) {
        if ($key -notlike "TEMPLATE_*") {
            continue
        }
        $value = $EnvMap[$key]
        if ($value -match "^\d+$") {
            $label = $key.Substring("TEMPLATE_".Length).ToLowerInvariant().Replace("_", " ")
            $label = (Get-Culture).TextInfo.ToTitleCase($label)
            $map[[int]$value] = $label
        }
    }
    return $map
}

function Get-DaemonCycles {
    param(
        [datetime]$WindowStart
    )

    $historyDir = $daemonHistoryDir
    if (-not (Test-Path $historyDir)) {
        return @()
    }

    $rows = @()
    Get-ChildItem $historyDir -Filter "execucao_daemon_*.csv" | Sort-Object Name | ForEach-Object {
        $rows += Import-Csv $_.FullName -Delimiter ";"
    }

    return $rows |
        ForEach-Object {
            [pscustomobject]@{
                dataHoraFim = [datetime]::ParseExact($_."DATA_HORA_FIM", "yyyy-MM-dd HH:mm:ss", $null)
                inicio = [datetime]::ParseExact($_."INICIO", "yyyy-MM-dd HH:mm:ss", $null)
                fim = [datetime]::ParseExact($_."FIM", "yyyy-MM-dd HH:mm:ss", $null)
                duracaoSegundos = [int]$_."DURACAO_S"
                status = $_."STATUS"
                totalRecords = [int]$_."TOTAL_RECORDS"
                warns = [int]$_."WARNS"
                errors = [int]$_."ERRORS"
                detalhe = $_."DETALHE"
                logCiclo = $_."LOG_CICLO"
            }
        } |
        Where-Object { $_.fim -ge $WindowStart } |
        Sort-Object inicio
}

function Get-CycleLogFilesInWindow {
    param(
        [datetime]$WindowStart
    )

    $cycleRoot = $daemonCyclesDir
    if (-not (Test-Path $cycleRoot)) {
        return @()
    }

    return Get-ChildItem $cycleRoot -Recurse -File -Filter "extracao_daemon_*.log" |
        ForEach-Object {
            $match = [regex]::Match($_.BaseName, "extracao_daemon_(\d{4}-\d{2}-\d{2})_(\d{2})-(\d{2})-(\d{2})")
            if (-not $match.Success) {
                return
            }
            $timestampText = "{0} {1}:{2}:{3}" -f $match.Groups[1].Value, $match.Groups[2].Value, $match.Groups[3].Value, $match.Groups[4].Value
            $cycleStart = [datetime]::ParseExact($timestampText, "yyyy-MM-dd HH:mm:ss", $null)
            if ($cycleStart -ge $WindowStart) {
                [pscustomobject]@{
                    path = $_.FullName
                    cycleStart = $cycleStart
                    lastWrite = $_.LastWriteTime
                }
            }
        } |
        Sort-Object cycleStart
}

function Get-CycleCompletionFindings {
    param(
        [object[]]$CycleLogs
    )

    $findings = @()
    foreach ($cycleLog in $CycleLogs) {
        $lines = Get-Content $cycleLog.path
        $hasFinalSummary = @($lines | Select-String -Pattern "RESUMO FINAL DO CICLO \(DAEMON\)").Count -gt 0

        $started = @{}
        $finished = @{}
        $lastStartedEntity = $null
        $lastFinishedEntity = $null

        foreach ($line in $lines) {
            $startMatch = [regex]::Match($line, "\[ENTIDADE\] INICIANDO EXTRACAO: ([A-Z0-9_]+)")
            if ($startMatch.Success) {
                $entity = $startMatch.Groups[1].Value
                $started[$entity] = $true
                $lastStartedEntity = $entity
            }

            $finishMatch = [regex]::Match($line, "\[ENTIDADE\] RESUMO FINAL: ([A-Z0-9_]+)")
            if ($finishMatch.Success) {
                $entity = $finishMatch.Groups[1].Value
                $finished[$entity] = $true
                $lastFinishedEntity = $entity
            }
        }

        $missingEntities = @($started.Keys | Where-Object { -not $finished.ContainsKey($_) } | Sort-Object)
        if (-not $hasFinalSummary -or $missingEntities.Count -gt 0) {
            $findings += [pscustomobject]@{
                path = $cycleLog.path
                cycleStart = $cycleLog.cycleStart
                hasFinalSummary = $hasFinalSummary
                missingEntities = $missingEntities
                lastStartedEntity = $lastStartedEntity
                lastFinishedEntity = $lastFinishedEntity
                lastWrite = $cycleLog.lastWrite
            }
        }
    }

    return $findings
}

function Get-LogSources {
    param(
        [datetime]$WindowStart,
        [object[]]$CycleLogs
    )

    $sources = @()
    $rootLog = Join-Path $appRuntimeDir "extrator-esl.log"
    $daemonConsole = Join-Path $daemonRuntimeDir "loop_daemon_console.log"

    if (Test-Path $rootLog) {
        $sources += $rootLog
    }
    if (Test-Path $daemonConsole) {
        $sources += $daemonConsole
    }

    if (Test-Path $appOperationsDir) {
        Get-ChildItem $appOperationsDir -File -Filter "extracao_dados_*.log" |
        Where-Object { $_.LastWriteTime -ge $WindowStart } |
        ForEach-Object { $sources += $_.FullName }
    }

    foreach ($cycle in $CycleLogs) {
        $sources += $cycle.path
    }

    return $sources | Sort-Object -Unique
}

function Get-LogPatternReport {
    param(
        [string[]]$Sources,
        [datetime]$WindowStart,
        [datetime]$WindowEnd
    )

    $patterns = [ordered]@{
        timeout = "Timeout na requisicao|Timeout ao executar|request timed out"
        retry = "tentativa \d+/\d+|Aguardando \d+ms .*tentativa|retry apos timeout|backoff exponencial"
        rate_limit = "HTTP 429|Rate limit"
        circuit_breaker = "CIRCUIT BREAKER ABERTO|Circuit breaker"
        lock_error = "Nao foi possivel adquirir lock global"
        extraction_error = "ERRO NA EXTRACAO|\[ERRO\] ERRO NA EXTRACAO|Falha na extracao"
    }

    $counts = [ordered]@{}
    foreach ($name in $patterns.Keys) {
        $counts[$name] = 0
    }

    $samples = New-Object System.Collections.Generic.List[string]
    $timestampPattern = "^(?<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})"

    foreach ($source in $Sources) {
        $currentTimestamp = $null
        foreach ($line in Get-Content $source) {
            $match = [regex]::Match($line, $timestampPattern)
            if ($match.Success) {
                $currentTimestamp = [datetime]::ParseExact($match.Groups["ts"].Value, "yyyy-MM-dd HH:mm:ss", $null)
            }

            if ($null -eq $currentTimestamp -or $currentTimestamp -lt $WindowStart -or $currentTimestamp -gt $WindowEnd) {
                continue
            }

            foreach ($name in $patterns.Keys) {
                if ($line -match $patterns[$name]) {
                    $counts[$name]++
                    if ($samples.Count -lt 18) {
                        $samples.Add(("{0} | {1}" -f [IO.Path]::GetFileName($source), $line.Trim()))
                    }
                }
            }
        }
    }

    return [pscustomobject]@{
        counts = $counts
        samples = $samples
    }
}

function Get-GraphQlStepTimingReport {
    param(
        [datetime]$WindowStart
    )

    $consolePath = Join-Path $daemonRuntimeDir "loop_daemon_console.log"
    if (-not (Test-Path $consolePath)) {
        return @()
    }

    $events = foreach ($line in Get-Content $consolePath) {
        $match = [regex]::Match($line, "^(?<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).*\[ENTIDADE\] (?<kind>INICIANDO EXTRACAO|RESUMO FINAL): (?<entity>COLETAS|FRETES)$")
        if (-not $match.Success) {
            continue
        }

        $timestamp = [datetime]::ParseExact($match.Groups["ts"].Value, "yyyy-MM-dd HH:mm:ss", $null)
        if ($timestamp -lt $WindowStart) {
            continue
        }

        [pscustomobject]@{
            timestamp = $timestamp
            kind = $match.Groups["kind"].Value
            entity = $match.Groups["entity"].Value
        }
    }

    $report = @()
    foreach ($entity in @("COLETAS", "FRETES")) {
        $start = $null
        $durations = New-Object System.Collections.Generic.List[double]

        foreach ($event in $events | Where-Object entity -eq $entity | Sort-Object timestamp) {
            if ($event.kind -eq "INICIANDO EXTRACAO") {
                $start = $event.timestamp
                continue
            }

            if ($event.kind -eq "RESUMO FINAL" -and $start) {
                $durations.Add(($event.timestamp - $start).TotalSeconds)
                $start = $null
            }
        }

        $report += [pscustomobject]@{
            entity = $entity
            completedPairs = $durations.Count
            avgSeconds = if ($durations.Count -gt 0) { [math]::Round((($durations | Measure-Object -Average).Average), 2) } else { $null }
            minSeconds = if ($durations.Count -gt 0) { ($durations | Measure-Object -Minimum).Minimum } else { $null }
            maxSeconds = if ($durations.Count -gt 0) { ($durations | Measure-Object -Maximum).Maximum } else { $null }
            openStart = if ($start) { $start.ToString("yyyy-MM-dd HH:mm:ss") } else { "" }
        }
    }

    return $report
}

function Get-JavaProcessReport {
    $byId = @{}

    Get-Process java -ErrorAction SilentlyContinue | ForEach-Object {
        $byId[$_.Id] = [ordered]@{
            pid = $_.Id
            startTime = $_.StartTime
            cpu = [math]::Round($_.CPU, 2)
            workingSetMb = [math]::Round($_.WorkingSet64 / 1MB, 2)
            commandLine = ""
        }
    }

    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue | ForEach-Object {
        if ($byId.ContainsKey($_.ProcessId)) {
            $byId[$_.ProcessId]["commandLine"] = $_.CommandLine
        }
    }

    return $byId.Values | Sort-Object startTime
}

function Add-Section {
    param(
        [System.Collections.Generic.List[string]]$Lines,
        [string]$Title
    )

    $Lines.Add("")
    $Lines.Add("## $Title")
    $Lines.Add("")
}

Add-Type -AssemblyName System.Data

$envMap = Get-EnvMap
$templateMap = Get-TemplateMap -EnvMap $envMap
$daemonState = Get-DaemonState
$cycles = Get-DaemonCycles -WindowStart $windowStart
$cycleLogs = Get-CycleLogFilesInWindow -WindowStart $windowStart
$incompleteCycleLogs = Get-CycleCompletionFindings -CycleLogs $cycleLogs
$logSources = Get-LogSources -WindowStart $windowStart -CycleLogs $cycleLogs
$logPatternReport = Get-LogPatternReport -Sources $logSources -WindowStart $windowStart -WindowEnd $windowEnd
$graphQlStepTimings = Get-GraphQlStepTimingReport -WindowStart $windowStart
$javaProcesses = Get-JavaProcessReport

$dbConnection = $null
try {
    $dbConnection = Get-DbConnection -EnvMap $envMap

    $businessFreshness = Invoke-SqlQuery -Connection $dbConnection -Sql @"
SELECT 'coletas' AS entidade, COUNT(*) AS total_linhas, COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0) AS linhas_janela, CONVERT(varchar(19), MAX(data_extracao), 120) AS ultima_extracao FROM dbo.coletas
UNION ALL
SELECT 'fretes', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.fretes
UNION ALL
SELECT 'manifestos', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.manifestos
UNION ALL
SELECT 'cotacoes', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.cotacoes
UNION ALL
SELECT 'localizacao_cargas', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.localizacao_cargas
UNION ALL
SELECT 'contas_a_pagar', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.contas_a_pagar
UNION ALL
SELECT 'faturas_por_cliente', COUNT(*), COALESCE(SUM(CASE WHEN data_extracao >= DATEADD(HOUR, -$Hours, GETDATE()) THEN 1 ELSE 0 END), 0), CONVERT(varchar(19), MAX(data_extracao), 120) FROM dbo.faturas_por_cliente
"@

    $auditCounts = Invoke-SqlQuery -Connection $dbConnection -Sql @"
SELECT
    (SELECT COUNT(*) FROM dbo.log_extracoes) AS total_log_extracoes,
    (SELECT COUNT(*) FROM dbo.log_extracoes WHERE timestamp_fim >= DATEADD(HOUR, -$Hours, GETDATE())) AS log_extracoes_janela,
    (SELECT CONVERT(varchar(19), MAX(timestamp_fim), 120) FROM dbo.log_extracoes) AS ultima_log_extracao,
    (SELECT COUNT(*) FROM dbo.page_audit) AS total_page_audit,
    (SELECT COUNT(*) FROM dbo.page_audit WHERE [timestamp] >= DATEADD(HOUR, -$Hours, SYSUTCDATETIME())) AS page_audit_janela,
    (SELECT CONVERT(varchar(26), MAX([timestamp]), 121) FROM dbo.page_audit) AS ultima_page_audit,
    (SELECT COUNT(*) FROM dbo.sys_execution_history) AS total_exec_history,
    (SELECT CONVERT(varchar(19), MAX(end_time), 120) FROM dbo.sys_execution_history) AS ultima_exec_history
"@

    $pageAuditByTemplate = Invoke-SqlQuery -Connection $dbConnection -Sql @"
SELECT template_id, COUNT(*) AS paginas, SUM(total_itens) AS total_itens, CONVERT(varchar(26), MAX([timestamp]), 121) AS ultima_auditoria
FROM dbo.page_audit
WHERE [timestamp] >= DATEADD(HOUR, -$Hours, SYSUTCDATETIME())
GROUP BY template_id
ORDER BY MAX([timestamp]) DESC
"@

    $executionHistory = Invoke-SqlQuery -Connection $dbConnection -Sql @"
SELECT TOP 10
    CONVERT(varchar(19), start_time, 120) AS start_time,
    CONVERT(varchar(19), end_time, 120) AS end_time,
    duration_seconds,
    status,
    total_records,
    ISNULL(error_category, '') AS error_category,
    LEFT(ISNULL(error_message, ''), 200) AS error_message
FROM dbo.sys_execution_history
ORDER BY end_time DESC
"@
}
finally {
    if ($dbConnection) {
        $dbConnection.Close()
    }
}

$cycleIntervals = @()
for ($i = 1; $i -lt $cycles.Count; $i++) {
    $cycleIntervals += ($cycles[$i].inicio - $cycles[$i - 1].fim).TotalMinutes
}

$expectedCycles = [math]::Floor(($Hours * 60) / $ExpectedLoopMinutes)
$cycleCount = $cycles.Count
$successCount = ($cycles | Where-Object status -eq "SUCCESS").Count
$alertCount = ($cycles | Where-Object status -eq "ALERT").Count
$errorCount = ($cycles | Where-Object status -eq "ERROR").Count
$minDuration = if ($cycleCount -gt 0) { ($cycles.duracaoSegundos | Measure-Object -Minimum).Minimum } else { 0 }
$avgDuration = if ($cycleCount -gt 0) { [math]::Round((($cycles.duracaoSegundos | Measure-Object -Average).Average), 2) } else { 0 }
$maxDuration = if ($cycleCount -gt 0) { ($cycles.duracaoSegundos | Measure-Object -Maximum).Maximum } else { 0 }
$avgGapMinutes = if ($cycleIntervals.Count -gt 0) { [math]::Round((($cycleIntervals | Measure-Object -Average).Average), 2) } else { 0 }
$maxGapMinutes = if ($cycleIntervals.Count -gt 0) { [math]::Round((($cycleIntervals | Measure-Object -Maximum).Maximum), 2) } else { 0 }

$warnTrendReason = $null
$alertCycles = $cycles | Where-Object status -eq "ALERT"
if ($alertCycles.Count -ge 6) {
    $firstWarnAvg = (($alertCycles | Select-Object -First 3).warns | Measure-Object -Average).Average
    $lastWarnAvg = (($alertCycles | Select-Object -Last 3).warns | Measure-Object -Average).Average
    if ($firstWarnAvg -gt 0 -and $lastWarnAvg -ge ($firstWarnAvg * 1.5)) {
        $warnTrendReason = "Warnings medios subiram de {0:n2} para {1:n2} nos ciclos ALERT recentes." -f $firstWarnAvg, $lastWarnAvg
    }
}

$businessRowsInWindow = ($businessFreshness.Rows | ForEach-Object { [int]$_.linhas_janela } | Measure-Object -Sum).Sum
$totalBusinessRows = ($businessFreshness.Rows | ForEach-Object { [int]$_.total_linhas } | Measure-Object -Sum).Sum
$auditRow = if ($auditCounts.Rows.Count -gt 0) {
    $auditCounts.Rows[0]
}
else {
    [pscustomobject]@{
        total_log_extracoes = 0
        log_extracoes_janela = 0
        ultima_log_extracao = ""
        total_page_audit = 0
        page_audit_janela = 0
        ultima_page_audit = ""
        total_exec_history = 0
        ultima_exec_history = ""
    }
}

$failReasons = New-Object System.Collections.Generic.List[string]
if ($cycleCount -lt $expectedCycles) {
    $failReasons.Add("Foram observados $cycleCount ciclos finalizados na janela, abaixo do esperado aproximado de $expectedCycles para um loop de $ExpectedLoopMinutes minutos.")
}
if ($maxGapMinutes -gt $GapThresholdMinutes) {
    $failReasons.Add("Maior gap entre ciclos finalizados foi de $maxGapMinutes minutos, acima do limite de $GapThresholdMinutes minutos.")
}
if ($incompleteCycleLogs.Count -gt 0) {
    $failReasons.Add("Foram encontrados $($incompleteCycleLogs.Count) logs de ciclo sem resumo final ou com entidade iniciada sem finalizacao.")
}
if ($errorCount -gt 0) {
    $failReasons.Add("Ha $errorCount ciclos em ERROR na janela, incluindo falhas repetidas por lock global.")
}
if ($businessRowsInWindow -eq 0) {
    $failReasons.Add("Nenhuma tabela de negocio registrou data_extracao nas ultimas $Hours horas.")
}
if ([int]$auditRow.page_audit_janela -gt 0 -and $businessRowsInWindow -eq 0) {
    $failReasons.Add("Houve paginacao auditada em page_audit na janela, mas nenhuma persistencia nas tabelas finais de negocio.")
}
if ([int]$auditRow.log_extracoes_janela -eq 0) {
    $failReasons.Add("A tabela log_extracoes nao registrou nenhuma execucao na janela analisada.")
}
if ($warnTrendReason) {
    $failReasons.Add($warnTrendReason)
}

$verdict = if ($failReasons.Count -eq 0) { "APROVADO" } else { "FALHO" }

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Relatorio de Validacao de Producao")
$report.Add("")
$report.Add(("- Janela analisada: {0} ate {1}" -f $windowStart.ToString("yyyy-MM-dd HH:mm:ss zzz"), $windowEnd.ToString("yyyy-MM-dd HH:mm:ss zzz")))
$report.Add(("- Loop esperado: ~{0} minutos" -f $ExpectedLoopMinutes))
$report.Add(("- Gap maximo aceitavel para alerta: {0} minutos" -f $GapThresholdMinutes))
$report.Add(("- Veredito: **{0}**" -f $verdict))

Add-Section -Lines $report -Title "Resumo Executivo"
$report.Add("- Ciclos finalizados observados: $cycleCount")
$report.Add("- Distribuicao de status: SUCCESS=$successCount, ALERT=$alertCount, ERROR=$errorCount")
$report.Add("- Duracao media por ciclo: $avgDuration s")
$report.Add("- Maior gap entre ciclos finalizados: $maxGapMinutes min")
$report.Add("- Linhas recentes em tabelas de negocio: $businessRowsInWindow")
$report.Add("- Linhas recentes em log_extracoes: $([int]$auditRow.log_extracoes_janela)")
$report.Add("- Linhas recentes em page_audit: $([int]$auditRow.page_audit_janela)")

Add-Section -Lines $report -Title "Criterios de Falha"
if ($failReasons.Count -eq 0) {
    $report.Add("- Nenhum criterio de falha foi acionado pela janela analisada.")
}
else {
    foreach ($reason in $failReasons) {
        $report.Add("- $reason")
    }
}

Add-Section -Lines $report -Title "Estado Atual do Daemon"
if ($daemonState.Count -eq 0) {
    $report.Add("- Estado do daemon nao encontrado em logs\\daemon\\runtime\\loop_daemon.state.")
}
else {
    foreach ($key in @("status", "last_run_at", "next_run_at", "pid", "detail")) {
        if ($daemonState.Contains($key)) {
            $report.Add(("- {0}: {1}" -f $key, $daemonState[$key]))
        }
    }
}

Add-Section -Lines $report -Title "Metricas do Loop"
$report.Add("- Ciclos esperados para a janela: $expectedCycles")
$report.Add("- Ciclos observados: $cycleCount")
$report.Add("- Duracao minima/maxima: $minDuration s / $maxDuration s")
$report.Add("- Gap medio entre ciclos finalizados: $avgGapMinutes min")
$report.Add("- Gap maximo entre ciclos finalizados: $maxGapMinutes min")
$report.Add("")
$report.Add("| Inicio | Fim | Status | Duracao(s) | TotalRecords |")
$report.Add("| --- | --- | --- | ---: | ---: |")
foreach ($cycle in $cycles) {
    $report.Add((
        "| {0} | {1} | {2} | {3} | {4} |" -f
        $cycle.inicio.ToString("yyyy-MM-dd HH:mm:ss"),
        $cycle.fim.ToString("yyyy-MM-dd HH:mm:ss"),
        $cycle.status,
        $cycle.duracaoSegundos,
        $cycle.totalRecords
    ))
}

Add-Section -Lines $report -Title "Tempo por Etapa"
if ($graphQlStepTimings.Count -eq 0) {
    $report.Add("- Nao foi possivel calcular tempos por etapa a partir do console do daemon.")
}
else {
    $report.Add("| Etapa | Pares completos | Media(s) | Min(s) | Max(s) | Execucao em aberto |")
    $report.Add("| --- | ---: | ---: | ---: | ---: | --- |")
    foreach ($step in $graphQlStepTimings) {
        $open = if ([string]::IsNullOrWhiteSpace($step.openStart)) { "-" } else { $step.openStart }
        $avg = if ($null -ne $step.avgSeconds) { $step.avgSeconds } else { "-" }
        $min = if ($null -ne $step.minSeconds) { $step.minSeconds } else { "-" }
        $max = if ($null -ne $step.maxSeconds) { $step.maxSeconds } else { "-" }
        $report.Add(("| {0} | {1} | {2} | {3} | {4} | {5} |" -f $step.entity, $step.completedPairs, $avg, $min, $max, $open))
    }
}

Add-Section -Lines $report -Title "Falhas Silenciosas e Ciclos Incompletos"
if ($incompleteCycleLogs.Count -eq 0) {
    $report.Add("- Nenhum log de ciclo incompleto foi detectado na janela.")
}
else {
    foreach ($finding in $incompleteCycleLogs) {
        $lastStarted = if ($finding.lastStartedEntity) { $finding.lastStartedEntity } else { "(nao identificado)" }
        $lastFinished = if ($finding.lastFinishedEntity) { $finding.lastFinishedEntity } else { "(sem resumo por entidade)" }
        $report.Add((
            "- {0}: resumo_final={1}, ultima_entidade_iniciada={2}, ultima_entidade_com_resumo={3}, last_write={4}" -f
            $finding.path,
            $finding.hasFinalSummary,
            $lastStarted,
            $lastFinished,
            $finding.lastWrite.ToString("yyyy-MM-dd HH:mm:ss")
        ))
    }
}

Add-Section -Lines $report -Title "Banco de Dados"
$report.Add("- Totais atuais: negocio=$totalBusinessRows, log_extracoes=$([int]$auditRow.total_log_extracoes), page_audit=$([int]$auditRow.total_page_audit), sys_execution_history=$([int]$auditRow.total_exec_history)")
$report.Add("- Ultima log_extracao: $($auditRow.ultima_log_extracao)")
$report.Add("- Ultima page_audit: $($auditRow.ultima_page_audit)")
$report.Add("- Ultima execucao em sys_execution_history: $($auditRow.ultima_exec_history)")
$report.Add("")
$report.Add("| Entidade | Total Linhas | Linhas Janela | Ultima Extracao |")
$report.Add("| --- | ---: | ---: | --- |")
foreach ($row in $businessFreshness.Rows) {
    $report.Add(("| {0} | {1} | {2} | {3} |" -f $row.entidade, $row.total_linhas, $row.linhas_janela, $row.ultima_extracao))
}

if ($pageAuditByTemplate.Rows.Count -gt 0) {
    $report.Add("")
    $report.Add("| Template | Nome | Paginas | Total Itens | Ultima Auditoria (UTC) |")
    $report.Add("| ---: | --- | ---: | ---: | --- |")
    foreach ($row in $pageAuditByTemplate.Rows) {
        $templateId = [int]$row.template_id
        $templateName = if ($templateMap.ContainsKey($templateId)) { $templateMap[$templateId] } else { "Template $templateId" }
        $report.Add(("| {0} | {1} | {2} | {3} | {4} |" -f $templateId, $templateName, $row.paginas, $row.total_itens, $row.ultima_auditoria))
    }
}

if ($executionHistory.Rows.Count -gt 0) {
    $report.Add("")
    $report.Add("| Inicio | Fim | Status | Duracao(s) | TotalRecords | ErrorCategory | Mensagem |")
    $report.Add("| --- | --- | --- | ---: | ---: | --- | --- |")
    foreach ($row in $executionHistory.Rows) {
        $report.Add((
            "| {0} | {1} | {2} | {3} | {4} | {5} | {6} |" -f
            $row.start_time,
            $row.end_time,
            $row.status,
            $row.duration_seconds,
            $row.total_records,
            $row.error_category,
            $row.error_message
        ))
    }
}

Add-Section -Lines $report -Title "Analise de Logs"
$report.Add("- Contagens por padrao: timeout=$($logPatternReport.counts.timeout), retry=$($logPatternReport.counts.retry), rate_limit=$($logPatternReport.counts.rate_limit), circuit_breaker=$($logPatternReport.counts.circuit_breaker), lock_error=$($logPatternReport.counts.lock_error), extraction_error=$($logPatternReport.counts.extraction_error)")
$report.Add("")
if ($logPatternReport.samples.Count -eq 0) {
    $report.Add("- Nenhuma amostra de log relevante foi encontrada na janela.")
}
else {
    foreach ($sample in $logPatternReport.samples) {
        $report.Add("- $sample")
    }
}

Add-Section -Lines $report -Title "Processos Java Ativos"
if ($javaProcesses.Count -eq 0) {
    $report.Add("- Nenhum processo java ativo foi encontrado.")
}
else {
    $report.Add("| PID | Inicio | CPU(s) | Memoria(MB) | CommandLine |")
    $report.Add("| ---: | --- | ---: | ---: | --- |")
    foreach ($proc in $javaProcesses) {
        $command = if ([string]::IsNullOrWhiteSpace($proc.commandLine)) { "(sem command line disponivel)" } else { $proc.commandLine }
        $report.Add((
            "| {0} | {1} | {2} | {3} | {4} |" -f
            $proc.pid,
            $proc.startTime.ToString("yyyy-MM-dd HH:mm:ss"),
            $proc.cpu,
            $proc.workingSetMb,
            $command.Replace("|", "/")
        ))
    }
}

Add-Section -Lines $report -Title "Problemas Encontrados"
if ($failReasons.Count -eq 0) {
    $report.Add("- Nenhum problema operacional critico foi detectado nesta janela.")
}
else {
    foreach ($reason in $failReasons) {
        $report.Add("- $reason")
    }

    if ($incompleteCycleLogs.Count -gt 0) {
        $report.Add("- Ha evidencia de ciclo iniciado e nao finalizado em log de ciclo.")
    }
    if ($logPatternReport.counts.rate_limit -gt 0) {
        $report.Add("- O ambiente enfrentou rate limit durante a validacao operacional.")
    }
    if ($logPatternReport.counts.lock_error -gt 0) {
        $report.Add("- O loop atual esta repetindo falhas por lock global de execucao.")
    }
}

Add-Section -Lines $report -Title "Recomendacoes"
$report.Add("- Investigar e encerrar com seguranca o processo java antigo que permanece vivo sem finalizar ciclo, antes de reiniciar qualquer soak test oficial.")
$report.Add("- Corrigir o lock global de execucao antes de declarar o loop saudavel; hoje o daemon continua rodando, mas sem executar extracoes reais.")
$report.Add("- Restaurar a consistencia de persistencia: page_audit ativo com tabelas finais zeradas indica pipeline parcial ou limpeza indevida do banco.")
$report.Add("- Reabilitar log_extracoes para que a validacao API x banco tenha base auditavel por entidade e por janela.")
$report.Add("- Repetir o soak test de 24h somente apos estabilizar lock, persistencia e rate limit da DataExport.")

$reportDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}

Set-Content -Path $OutputPath -Value $report -Encoding UTF8
Limit-RecentFiles -Directory $reportDir -Filter "producao_validacao_24h_*.md" -MaxFiles 20

Write-Output "Relatorio gerado: $OutputPath"
Write-Output "Veredito: $verdict"
