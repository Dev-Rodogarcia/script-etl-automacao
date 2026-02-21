@echo off
setlocal EnableExtensions

set "BASE_DIR=%~dp0"
set "LOGS_DIR=%BASE_DIR%logs"

echo Limpando arquivos de log em: "%LOGS_DIR%"
echo Observacao: arquivos .csv serao preservados.

if not exist "%LOGS_DIR%" (
    mkdir "%LOGS_DIR%" 2>nul
)

for /r "%LOGS_DIR%" %%F in (*.log) do (
    del /f /q "%%~fF" 2>nul
)

mkdir "%LOGS_DIR%\daemon\ciclos" 2>nul
mkdir "%LOGS_DIR%\daemon\history" 2>nul
mkdir "%LOGS_DIR%\daemon\reconciliacao" 2>nul
mkdir "%LOGS_DIR%\daemon\runtime" 2>nul

echo Limpeza concluida.
exit /b 0
