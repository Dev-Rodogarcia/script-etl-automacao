@echo off
setlocal EnableExtensions
REM ==[DOC-FILE]===============================================================
REM Arquivo : limpar_logs.bat
REM Tipo    : Script operacional Windows (.bat)
REM Papel   : Limpa completamente a arvore de logs e recria apenas a estrutura
REM           base de pastas para a proxima execucao.
REM
REM Fluxo geral:
REM 1) Resolve a raiz do projeto e a pasta real de logs.
REM 2) Bloqueia a limpeza se o daemon aparentar estar em execucao.
REM 3) Remove todos os arquivos e subpastas dentro de logs.
REM 4) Recria somente as pastas padrao esperadas pela aplicacao.
REM 5) Opcionalmente limpa temporarios extras fora de logs no modo /full.
REM
REM Parametros:
REM - /full  : tambem remove temporarios locais fora de logs.
REM - /force : ignora a verificacao de daemon ativo.
REM
REM Variaveis-chave:
REM - REPO_ROOT: raiz detectada do projeto.
REM - LOGS_DIR: pasta alvo da limpeza.
REM - FULL_CLEAN: habilita limpeza adicional fora de logs.
REM - FORCE_CLEAN: permite limpar mesmo se houver indicio de daemon ativo.
REM [DOC-FILE-END]===========================================================

set "FULL_CLEAN=0"
set "FORCE_CLEAN=0"

:PARSE_ARGS
if "%~1"=="" goto ARGS_DONE
if /i "%~1"=="/full" (
    set "FULL_CLEAN=1"
    shift
    goto PARSE_ARGS
)
if /i "%~1"=="/force" (
    set "FORCE_CLEAN=1"
    shift
    goto PARSE_ARGS
)
if /i "%~1"=="/?" goto USAGE
if /i "%~1"=="-h" goto USAGE
echo [AVISO] Parametro desconhecido ignorado: %~1
shift
goto PARSE_ARGS

:ARGS_DONE
call :RESOLVE_ROOT
if errorlevel 1 exit /b 1

echo ================================================================
echo LIMPEZA TOTAL DE LOGS
echo ================================================================
echo Projeto : "%REPO_ROOT%"
echo Logs    : "%LOGS_DIR%"
echo.

call :CHECK_DAEMON_RUNNING
if errorlevel 1 exit /b 1

if not exist "%LOGS_DIR%" (
    mkdir "%LOGS_DIR%" 2>nul
)

call :COUNT_FILES "%LOGS_DIR%" FILES_BEFORE
echo Arquivos encontrados antes da limpeza: %FILES_BEFORE%

attrib -r -h -s "%LOGS_DIR%\*" /s /d >nul 2>&1
del /a /f /q /s "%LOGS_DIR%\*" >nul 2>&1

for /f "delims=" %%D in ('dir /b /a:d "%LOGS_DIR%" 2^>nul') do (
    rd /s /q "%LOGS_DIR%\%%D" >nul 2>&1
)

call :ENSURE_LOG_STRUCTURE

if "%FULL_CLEAN%"=="1" (
    echo.
    echo [Modo FULL] Limpando temporarios locais adicionais...
    call :FULL_CLEAN_EXTRA
)

call :COUNT_FILES "%LOGS_DIR%" FILES_AFTER
echo.
echo Limpeza concluida.
echo Arquivos restantes em logs: %FILES_AFTER%
if not "%FILES_AFTER%"=="0" (
    echo [AVISO] Ainda restaram arquivos em logs. Verifique arquivos em uso ou permissoes.
    exit /b 1
)
echo Estrutura recriada e pronta para novos logs.
if "%FULL_CLEAN%"=="0" (
    echo Dica: use "limpar_logs.bat /full" para limpar tambem caches e temporarios fora de logs.
)
exit /b 0

:USAGE
echo Uso:
echo   limpar_logs.bat
echo   limpar_logs.bat /full
echo   limpar_logs.bat /force
echo   limpar_logs.bat /full /force
echo.
echo Padrao : limpa recursivamente TODO o conteudo de logs e recria apenas as pastas base.
echo /full  : inclui temporarios locais fora de logs.
echo /force : ignora a verificacao de daemon aparentemente ativo.
exit /b 0

:RESOLVE_ROOT
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%.") do set "SCRIPT_DIR=%%~fI"

if defined EXTRATOR_REPO_ROOT (
    set "REPO_ROOT=%EXTRATOR_REPO_ROOT%"
) else (
    set "REPO_ROOT=%SCRIPT_DIR%"
    if not exist "%REPO_ROOT%\pom.xml" (
        for %%I in ("%SCRIPT_DIR%\..\..") do set "REPO_ROOT=%%~fI"
    )
)

if not exist "%REPO_ROOT%\pom.xml" (
    echo ERRO: nao foi possivel localizar a raiz do projeto.
    echo Defina EXTRATOR_REPO_ROOT se estiver executando o script fora do repositorio.
    exit /b 1
)

if defined EXTRATOR_LOGS_DIR (
    set "LOGS_DIR=%EXTRATOR_LOGS_DIR%"
) else (
    set "LOGS_DIR=%REPO_ROOT%\logs"
)

for %%I in ("%LOGS_DIR%") do set "LOGS_DIR=%%~fI"
exit /b 0

:CHECK_DAEMON_RUNNING
set "DAEMON_STATUS="
set "DAEMON_PID="
set "DAEMON_RUNNING=0"
set "STATE_FILE=%LOGS_DIR%\daemon\loop_daemon.state"
set "PID_FILE=%LOGS_DIR%\daemon\loop_daemon.pid"

if exist "%STATE_FILE%" call :READ_PROPERTY "%STATE_FILE%" "status" DAEMON_STATUS
if exist "%STATE_FILE%" if not defined DAEMON_PID call :READ_PROPERTY "%STATE_FILE%" "pid" DAEMON_PID
if exist "%PID_FILE%" set /p DAEMON_PID=<"%PID_FILE%"

if not defined DAEMON_PID exit /b 0

call :IS_PID_RUNNING "%DAEMON_PID%" DAEMON_RUNNING
if "%DAEMON_RUNNING%"=="0" exit /b 0

if "%FORCE_CLEAN%"=="1" (
    echo [AVISO] O daemon aparenta estar ativo ^(pid=%DAEMON_PID%, status=%DAEMON_STATUS%^), mas a limpeza seguira por /force.
    exit /b 0
)

if not defined DAEMON_STATUS set "DAEMON_STATUS=DESCONHECIDO"
if /i not "%DAEMON_STATUS%"=="STOPPED" (
    echo ERRO: o loop daemon aparenta estar em execucao ^(pid=%DAEMON_PID%, status=%DAEMON_STATUS%^).
    echo Pare o daemon antes de limpar os logs ou rode "limpar_logs.bat /force" por sua conta e risco.
    exit /b 1
)
exit /b 0

:READ_PROPERTY
set "%~3="
for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"%~2=" "%~1" 2^>nul`) do (
    set "%~3=%%B"
)
exit /b 0

:IS_PID_RUNNING
set "%~2=0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-Process -Id %~1 -ErrorAction SilentlyContinue) { exit 0 } exit 1" >nul 2>&1
if not errorlevel 1 set "%~2=1"
exit /b 0

:COUNT_FILES
set "%~2=0"
if not exist "%~1" exit /b 0
for /f %%N in ('dir /b /s /a-d "%~1" 2^>nul ^| find /c /v ""') do set "%~2=%%N"
exit /b 0

:ENSURE_LOG_STRUCTURE
mkdir "%LOGS_DIR%" 2>nul
mkdir "%LOGS_DIR%\history" 2>nul
mkdir "%LOGS_DIR%\isolated_steps" 2>nul
mkdir "%LOGS_DIR%\daemon" 2>nul
mkdir "%LOGS_DIR%\daemon\ciclos" 2>nul
mkdir "%LOGS_DIR%\daemon\history" 2>nul
mkdir "%LOGS_DIR%\daemon\reconciliacao" 2>nul
mkdir "%LOGS_DIR%\daemon\runtime" 2>nul
exit /b 0

:FULL_CLEAN_EXTRA
if exist "%REPO_ROOT%\target\producao_bat_test" (
    rd /s /q "%REPO_ROOT%\target\producao_bat_test" >nul 2>&1
)

del /q "%REPO_ROOT%\target\tmp_*.*" >nul 2>&1
del /q "%REPO_ROOT%\target\loop_after_fix.log" >nul 2>&1

if exist "%REPO_ROOT%\src\dashboards\__pycache__" (
    rd /s /q "%REPO_ROOT%\src\dashboards\__pycache__" >nul 2>&1
)
if exist "%REPO_ROOT%\scripts\__pycache__" (
    rd /s /q "%REPO_ROOT%\scripts\__pycache__" >nul 2>&1
)

del /q "%REPO_ROOT%\last_run.properties" >nul 2>&1
exit /b 0
