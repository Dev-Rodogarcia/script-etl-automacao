param(
    [string] $RepoRoot
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir '..\..'))
}

$RepoRoot = [System.IO.Path]::GetFullPath($RepoRoot)
$JarPath = Join-Path $RepoRoot 'target\extrator.jar'
$LogDir = Join-Path $RepoRoot 'logs\fechamento-mensal'
$FailureMarker = Join-Path $LogDir 'ultimo_erro_fechamento_mensal.txt'
$RunStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$LogPath = Join-Path $LogDir ("fechamento-mensal-$RunStamp.log")
$EventSource = 'ETL-Fechamento-Mensal'

function Write-LogLine {
    param([string] $Message)
    $line = "[{0}] {1}" -f (Get-Date -Format o), $Message
    $line | Tee-Object -FilePath $LogPath -Append
}

function Import-OperationalEnvironment {
    $keys = @(
        'DB_URL',
        'DB_USER',
        'DB_PASSWORD',
        'API_BASEURL',
        'API_REST_TOKEN',
        'API_GRAPHQL_TOKEN',
        'API_DATAEXPORT_TOKEN'
    )

    foreach ($scope in @('Machine', 'User')) {
        foreach ($key in $keys) {
            $value = [System.Environment]::GetEnvironmentVariable($key, $scope)
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
            }
        }
    }
}

function Resolve-JavaCommand {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $javaHomeExe -PathType Leaf) {
            return $javaHomeExe
        }
    }
    return 'java.exe'
}

function Publish-FailureEvent {
    param(
        [int] $ExitCode,
        [string] $Message
    )

    $eventMessage = $Message
    if ($eventMessage.Length -gt 900) {
        $eventMessage = $eventMessage.Substring(0, 900)
    }

    try {
        & eventcreate.exe /T ERROR /ID 3315 /L APPLICATION /SO $EventSource /D $eventMessage | Out-Null
        Write-LogLine "Evento de falha publicado no Application Log com source $EventSource."
    } catch {
        Write-LogLine ("Nao foi possivel publicar evento de falha: {0}" -f $_.Exception.Message)
    }

    try {
        Set-Content -LiteralPath $FailureMarker -Encoding UTF8 -Value @(
            "exit_code=$ExitCode",
            "log=$LogPath",
            "timestamp=$(Get-Date -Format o)",
            "message=$Message"
        )
    } catch {
        Write-LogLine ("Nao foi possivel atualizar marcador de falha: {0}" -f $_.Exception.Message)
    }
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
Set-Location -LiteralPath $RepoRoot
Import-OperationalEnvironment

Write-LogLine "Iniciando fechamento mensal."
Write-LogLine "RepoRoot=$RepoRoot"
Write-LogLine "JarPath=$JarPath"

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    $message = "JAR nao encontrado em $JarPath. Execute mvn package -DskipTests antes do fechamento mensal."
    Write-LogLine $message
    Publish-FailureEvent -ExitCode 2 -Message $message
    $exitCode = 2
} else {
    $javaExe = Resolve-JavaCommand
    $javaArgs = @(
        '--enable-native-access=ALL-UNNAMED',
        "-DETL_BASE_DIR=$RepoRoot",
        "-Detl.base.dir=$RepoRoot",
        '-jar',
        $JarPath,
        '--fechamento-mensal'
    )

    Write-LogLine ("Executando: {0} {1}" -f $javaExe, (($javaArgs | ForEach-Object {
        if ($_ -like '* *') { '"' + $_ + '"' } else { $_ }
    }) -join ' '))

    & $javaExe @javaArgs *>> $LogPath
    $exitCode = if ($null -eq $LASTEXITCODE) { 0 } else { [int] $LASTEXITCODE }
}

Write-LogLine "Fechamento mensal finalizado com exit code $exitCode."

if ($exitCode -eq 0) {
    if (Test-Path -LiteralPath $FailureMarker -PathType Leaf) {
        Remove-Item -LiteralPath $FailureMarker -Force
    }
    Write-LogLine "Fechamento mensal concluido com sucesso."
    exit 0
}

$failureMessage = "Fechamento mensal falhou com exit code $exitCode. Consulte $LogPath."
Write-LogLine $failureMessage
Publish-FailureEvent -ExitCode $exitCode -Message $failureMessage
exit $exitCode
