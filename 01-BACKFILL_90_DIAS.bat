@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul

set "SCRIPT_ROOT=%~dp0"
for %%I in ("%SCRIPT_ROOT%.") do set "REPO_ROOT=%%~fI"

pushd "%REPO_ROOT%" >nul 2>&1
if errorlevel 1 (
    echo ERRO: Nao foi possivel acessar a raiz do repositorio: %REPO_ROOT%
    endlocal & exit /b 1
)

for /f "tokens=1,2" %%A in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "$hoje=(Get-Date).Date; $inicio=$hoje.AddDays(-90); Write-Output ($inicio.ToString('yyyy-MM-dd') + ' ' + $hoje.ToString('yyyy-MM-dd'))"') do (
    set "DATA_INICIO=%%A"
    set "DATA_FIM=%%B"
)

if not defined DATA_INICIO (
    echo ERRO: Nao foi possivel calcular a data inicial do backfill.
    set "BACKFILL_EXIT=1"
    goto :END
)

if not defined DATA_FIM (
    echo ERRO: Nao foi possivel calcular a data final do backfill.
    set "BACKFILL_EXIT=1"
    goto :END
)

echo ================================================================
echo BACKFILL AUTOMATICO - ULTIMOS 90 DIAS
echo ================================================================
echo Data de Inicio: !DATA_INICIO!
echo Data de Fim:    !DATA_FIM!
echo.
echo A rotina de producao executara:
echo   java -jar target\extrator.jar --extracao-intervalo !DATA_INICIO! !DATA_FIM! %*
echo.

call "%REPO_ROOT%\00-PRODUCAO_START.bat" --auto-intervalo "!DATA_INICIO!" "!DATA_FIM!" %*
set "BACKFILL_EXIT=!ERRORLEVEL!"

:END
popd >nul 2>&1
endlocal & exit /b %BACKFILL_EXIT%
