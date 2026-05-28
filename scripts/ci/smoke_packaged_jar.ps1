$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Path $PSScriptRoot -Parent | Split-Path -Parent
Set-Location $repoRoot

function Get-JavaMajorVersion {
    param([string]$JavaPath)

    $versionOutput = & cmd.exe /c "`"$JavaPath`" -version 2>&1" | Out-String
    if ($versionOutput -match 'version "1\.(\d+)') {
        return [int]$matches[1]
    }
    if ($versionOutput -match 'version "(\d+)') {
        return [int]$matches[1]
    }
    return 0
}

function Resolve-JavaCommand {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME "bin\java.exe"
    }

    $pathJava = Get-Command java -ErrorAction SilentlyContinue
    if ($pathJava) {
        $candidates += $pathJava.Source
    }

    foreach ($root in @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files (x86)\Java",
        "C:\Program Files (x86)\Eclipse Adoptium"
    )) {
        if (Test-Path $root) {
            $candidates += Get-ChildItem -Path $root -Directory -Filter "jdk*" -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName "bin\java.exe" }
        }
    }

    foreach ($candidate in ($candidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique)) {
        if ((Get-JavaMajorVersion $candidate) -ge 17) {
            return $candidate
        }
    }

    throw "ERRO: Java 17+ nao encontrado. Configure JAVA_HOME ou instale um JDK compativel."
}

function Invoke-Java {
    param([string[]]$Arguments)

    & $javaCmd @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ERRO: comando Java falhou: $($Arguments -join ' ')"
    }
}

$javaCmd = Resolve-JavaCommand

$jarPath = Join-Path $repoRoot "target\extrator.jar"
if (-not (Test-Path $jarPath)) {
    throw "ERRO: JAR nao encontrado em $jarPath"
}

$smokeDir = Join-Path $repoRoot "target\ci\security-smoke"
if (Test-Path $smokeDir) {
    Remove-Item -Recurse -Force $smokeDir
}
New-Item -ItemType Directory -Path $smokeDir | Out-Null

Write-Host "[smoke] Usando Java: $javaCmd"

Write-Host "[smoke] Validando comando de ajuda no JAR empacotado..."
Invoke-Java -Arguments @("-jar", $jarPath, "--ajuda") | Out-Null

Write-Host "[smoke] Validando inicializacao do modulo de seguranca (SQLite) no JAR empacotado..."
Invoke-Java -Arguments @("-Dextrator.security.db.path=$smokeDir\users.db", "-jar", $jarPath, "--auth-info") | Out-Null

$dbFile = Join-Path $smokeDir "users.db"
if (-not (Test-Path $dbFile)) {
    throw "ERRO: Banco SQLite de seguranca nao foi criado pelo smoke test."
}

Write-Host "[smoke] OK: JAR empacotado validado com sucesso."
