@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul

REM ================================================================
REM BACKFILL HISTORICO POS-DDL
REM
REM Uso operacional:
REM   Execute este script apos recriar/preparar o banco via DDL.
REM   O periodo e calculado automaticamente como ultimos 90 dias:
REM     DATA_INICIO = hoje - 90 dias
REM     DATA_FIM    = hoje
REM
REM Nao execute stored procedures de materializacao neste arquivo.
REM A orquestracao e centralizada em 00-PRODUCAO_START.bat, que
REM materializa as Fatos BI no POST-RUN quando a extracao conclui.
REM ================================================================
set "BACKFILL_EXIT=0"
set "DATA_INICIO="
set "DATA_FIM="

set "SCRIPT_ROOT=%~dp0"
for %%I in ("%SCRIPT_ROOT%.") do set "REPO_ROOT=%%~fI"

pushd "%REPO_ROOT%" >nul 2>&1
if errorlevel 1 (
    echo ERRO: Nao foi possivel acessar a raiz do repositorio: %REPO_ROOT%
    endlocal & exit /b 1
)

for /f "delims=" %%D in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Date (Get-Date).AddDays(-90) -Format yyyy-MM-dd"') do set "DATA_INICIO=%%D"
for /f "delims=" %%D in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Date -Format yyyy-MM-dd"') do set "DATA_FIM=%%D"

if not defined DATA_INICIO (
    echo ERRO: Nao foi possivel calcular DATA_INICIO automaticamente.
    set "BACKFILL_EXIT=1"
    goto :END
)

if not defined DATA_FIM (
    echo ERRO: Nao foi possivel calcular DATA_FIM automaticamente.
    set "BACKFILL_EXIT=1"
    goto :END
)

echo ================================================================
echo BACKFILL HISTORICO POS-DDL
echo ================================================================
echo ATENCAO: Este script deve ser executado apos a recriacao do banco de dados ^(DDL^). A carga dos ultimos 90 dias sera iniciada.
echo.
echo Data de Inicio: !DATA_INICIO!
echo Data de Fim:    !DATA_FIM!
echo.
echo A rotina de producao executara o fluxo oficial de extracao por intervalo:
echo   call "%REPO_ROOT%\00-PRODUCAO_START.bat" --auto-intervalo !DATA_INICIO! !DATA_FIM! %*
echo.
echo A materializacao das Fatos BI sera executada apenas pelo POST-RUN do 00-PRODUCAO_START.bat.
echo.
pause

call "%REPO_ROOT%\00-PRODUCAO_START.bat" --auto-intervalo "!DATA_INICIO!" "!DATA_FIM!" %*
set "BACKFILL_EXIT=!ERRORLEVEL!"

:END
popd >nul 2>&1
endlocal & exit /b %BACKFILL_EXIT%
