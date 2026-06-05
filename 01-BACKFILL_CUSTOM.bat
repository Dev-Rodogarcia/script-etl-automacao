@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul

REM ================================================================
REM BACKFILL CUSTOMIZADO
REM Edite manualmente o periodo abaixo antes de executar.
REM Formato esperado: YYYY-MM-DD
REM ================================================================
set "DATA_INICIO=2026-05-05"
set "DATA_FIM=2026-06-04"

set "SCRIPT_ROOT=%~dp0"
for %%I in ("%SCRIPT_ROOT%.") do set "REPO_ROOT=%%~fI"

pushd "%REPO_ROOT%" >nul 2>&1
if errorlevel 1 (
    echo ERRO: Nao foi possivel acessar a raiz do repositorio: %REPO_ROOT%
    endlocal & exit /b 1
)

if not defined DATA_INICIO (
    echo ERRO: DATA_INICIO nao foi definida no bloco inicial do arquivo.
    set "BACKFILL_EXIT=1"
    goto :END
)

if not defined DATA_FIM (
    echo ERRO: DATA_FIM nao foi definida no bloco inicial do arquivo.
    set "BACKFILL_EXIT=1"
    goto :END
)

echo ================================================================
echo BACKFILL CUSTOMIZADO
echo ================================================================
echo Data de Inicio: !DATA_INICIO!
echo Data de Fim:    !DATA_FIM!
echo.
echo A rotina de producao executara o mesmo fluxo da extracao por intervalo:
echo   java -jar target\extrator.jar --extracao-intervalo !DATA_INICIO! !DATA_FIM! %*
echo.

call "%REPO_ROOT%\00-PRODUCAO_START.bat" --auto-intervalo "!DATA_INICIO!" "!DATA_FIM!" %*
set "BACKFILL_EXIT=!ERRORLEVEL!"

:END
popd >nul 2>&1
endlocal & exit /b %BACKFILL_EXIT%
