param(
    [int]$Hours = 24,
    [int]$PollMinutes = 5,
    [switch]$ComFaturasGraphql,
    [switch]$PularValidacoesApiBanco,
    [string]$OutputPath,
    [int]$ShutdownTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Path $PSScriptRoot -Parent | Split-Path -Parent
Set-Location $repoRoot

$jarPath = Join-Path $repoRoot "target\extrator.jar"
if (-not (Test-Path $jarPath)) {
    throw "ERRO: JAR nao encontrado em $jarPath"
}

$reportScript = Join-Path $repoRoot "scripts\ci\relatorio_validacao_producao_24h.ps1"
if (-not (Test-Path $reportScript)) {
    throw "ERRO: Script de relatorio nao encontrado em $reportScript"
}

$daemonDir = Join-Path $repoRoot "logs\daemon"
New-Item -ItemType Directory -Force -Path $daemonDir | Out-Null
$statusLog = Join-Path $daemonDir ("soak_status_{0}.log" -f (Get-Date -Format "yyyy-MM-dd_HH-mm-ss"))

$flagFaturas = if ($ComFaturasGraphql) { @() } else { @("--sem-faturas-graphql") }
$modoFaturas = if ($ComFaturasGraphql) { "com faturas GraphQL" } else { "sem faturas GraphQL" }

function Invoke-Extrator {
    param(
        [string]$Titulo,
        [string[]]$Args,
        [switch]$IgnoreFailure
    )

    Write-Host ""
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Titulo"
    Write-Host ("Comando: java -jar target\\extrator.jar {0}" -f ($Args -join " "))

    & java -jar $jarPath @Args
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0 -and -not $IgnoreFailure) {
        throw "Falha na etapa '$Titulo' (exit code=$exitCode)."
    }

    return $exitCode
}

function Get-DaemonState {
    $stateFile = Join-Path $repoRoot "logs\daemon\loop_daemon.state"
    $map = @{}
    if (-not (Test-Path $stateFile)) {
        return $map
    }

    foreach ($line in Get-Content $stateFile) {
        if ($line -match "^\s*#" -or $line -notmatch "=") {
            continue
        }
        $parts = $line -split "=", 2
        $map[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $map
}

function Write-StatusSnapshot {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $estado = Get-DaemonState
    $status = if ($estado.ContainsKey("status")) { $estado["status"] } else { "N/A" }
    $updatedAt = if ($estado.ContainsKey("updated_at")) { $estado["updated_at"] } else { "N/A" }
    $detail = if ($estado.ContainsKey("detail")) { $estado["detail"] } else { "N/A" }
    $line = "[{0}] status={1} updated_at={2} detail={3}" -f $stamp, $status, $updatedAt, $detail
    $line | Tee-Object -FilePath $statusLog -Append
}

function Wait-DaemonStop {
    param(
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds([Math]::Max(5, $TimeoutSeconds))
    while ((Get-Date) -lt $deadline) {
        $estado = Get-DaemonState
        $status = if ($estado.ContainsKey("status")) { $estado["status"] } else { "" }
        if ($status -eq "STOPPED") {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

$started = $false
$endTime = (Get-Date).AddHours([Math]::Abs($Hours))

try {
    Write-Host "Soak test do loop daemon iniciado ($modoFaturas)."
    Write-Host "Duracao alvo: $Hours hora(s). Polling a cada $PollMinutes minuto(s)."
    Write-Host "Log de snapshots: $statusLog"

    Invoke-Extrator -Titulo "Etapa 1/5 - Iniciar loop daemon" -Args (@("--loop-daemon-start") + $flagFaturas)
    $started = $true

    Write-StatusSnapshot

    while ((Get-Date) -lt $endTime) {
        $sleepSeconds = [Math]::Min([Math]::Max(15, $PollMinutes * 60), [int][Math]::Ceiling(($endTime - (Get-Date)).TotalSeconds))
        if ($sleepSeconds -le 0) {
            break
        }

        Start-Sleep -Seconds $sleepSeconds
        Invoke-Extrator -Titulo "Etapa 2/5 - Status do daemon" -Args @("--loop-daemon-status") -IgnoreFailure | Out-Null
        Write-StatusSnapshot
    }
}
finally {
    if ($started) {
        Invoke-Extrator -Titulo "Etapa 3/5 - Solicitar parada do daemon" -Args @("--loop-daemon-stop") -IgnoreFailure | Out-Null
        if (-not (Wait-DaemonStop -TimeoutSeconds $ShutdownTimeoutSeconds)) {
            Write-Warning "Daemon nao confirmou STOPPED dentro de $ShutdownTimeoutSeconds segundos."
        }
        Invoke-Extrator -Titulo "Etapa 4/5 - Status final do daemon" -Args @("--loop-daemon-status") -IgnoreFailure | Out-Null
        Write-StatusSnapshot
    }

    Write-Host ""
    Write-Host "Etapa 5/5 - Gerando relatorio operacional 24h"
    $reportArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $reportScript, "-Hours", ([Math]::Abs($Hours)))
    if ($OutputPath) {
        $reportArgs += @("-OutputPath", $OutputPath)
    }
    & powershell @reportArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Falha ao gerar relatorio operacional (exit code=$LASTEXITCODE)."
    }

    if (-not $PularValidacoesApiBanco) {
        Invoke-Extrator -Titulo "Validacao API x banco detalhada" -Args (@("--validar-api-banco-24h-detalhado") + $flagFaturas) -IgnoreFailure | Out-Null
        Invoke-Extrator -Titulo "Validacao API x banco resumida" -Args (@("--validar-api-banco-24h") + $flagFaturas) -IgnoreFailure | Out-Null
    }
}

Write-Host ""
Write-Host "Soak test do loop daemon finalizado."
Write-Host "Snapshots de status: $statusLog"
