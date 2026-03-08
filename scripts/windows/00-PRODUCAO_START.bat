@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul
pushd "%~dp0"

set "JAR_PATH=%~dp0target\extrator.jar"
set "STARTUP_READY=0"
set "SQLITE_AUTH_DB="

REM ----------------------------------------------------------------
REM PRODUCAO: apenas executa artefato ja gerado.
REM Nao compila, nao testa, nao empacota.
REM Para compilar, use o ambiente de desenvolvimento separadamente.
REM ----------------------------------------------------------------
set "PROD_MODE=1"

:MENU
cls
echo ================================================================
echo            MENU DE PRODUCAO - EXTRATOR ESL CLOUD
echo            suporte: lucasmac.dev@gmail.com
echo            by: @valentelucass
echo ================================================================
echo.
echo 01. Extracao completa ultimas 24h
echo 02. Loop de extracao 30 minutos
echo 03. Extracao por intervalo
echo 04. Testar API especifica
echo 05. Validar configuracoes
echo 06. Relatorio completo de validacao
echo 07. Exportar CSV
echo 08. Auditar estrutura das APIs
echo 09. Ver ajuda de comandos
echo 10. Gerenciar usuarios de acesso ^(tecla U^)
echo 00. Sair
echo.
if not "%STARTUP_READY%"=="1" (
    echo Ambiente sera validado ao executar a primeira opcao.
    echo.
)
call :READ_MENU_OPTION
if errorlevel 2 (
    echo.
    echo Opcao invalida.
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
if errorlevel 1 (
    echo.
    echo Entrada encerrada. Encerrando menu de producao.
    goto :END
)

if "%OP%"=="1" goto :RUN_01
if "%OP%"=="2" goto :RUN_05
if "%OP%"=="3" goto :RUN_04
if "%OP%"=="4" goto :RUN_02
if "%OP%"=="5" goto :RUN_03
if "%OP%"=="6" goto :RUN_06
if "%OP%"=="7" goto :RUN_07
if "%OP%"=="8" goto :RUN_08
if "%OP%"=="9" goto :RUN_AJUDA
if "%OP%"=="10" goto :RUN_09
if "%OP%"=="0" goto :TRY_EXIT
if "%OP%"=="00" goto :TRY_EXIT

echo.
echo Opcao invalida.
timeout /t 2 /nobreak >nul 2>&1
goto :MENU

:RUN_01
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp001-executar_extracao_completa.bat"
goto :MENU

:RUN_02
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp002-testar_api_especifica.bat"
goto :MENU

:RUN_03
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp003-validar_config.bat"
goto :MENU

:RUN_04
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp004-extracao_por_intervalo.bat"
goto :MENU

:RUN_05
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp005-loop_extracao_30min.bat"
goto :MENU

:RUN_06
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp006-relatorio-completo-validacao.bat"
goto :MENU

:RUN_07
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp007-exportar_csv.bat"
goto :MENU

:RUN_08
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp008-auditar_api.bat"
goto :MENU

:RUN_09
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call "%~dp009-gerenciar_usuarios.bat"
goto :MENU

:RUN_AJUDA
call :PREPARE_PRODUCTION
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_AJUDA "Visualizar ajuda"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
echo Executando: java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --ajuda
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --ajuda
echo.
pause
goto :MENU

:TRY_EXIT
if "%STARTUP_READY%"=="1" if exist "%JAR_PATH%" (
    call :AUTH_CHECK MENU_EXIT "Sair do menu principal"
    if errorlevel 1 (
        timeout /t 2 /nobreak >nul 2>&1
        goto :MENU
    )
)
goto :END

:PREPARE_PRODUCTION
if "%STARTUP_READY%"=="1" exit /b 0

call :ensure_java
call :check_jar
if errorlevel 1 exit /b 1

call :check_sqlite_auth_db
if errorlevel 1 exit /b 1

if exist "%~dp0database\executar_database.bat" (
    echo.
    echo Preparando ambiente do banco...
    set "EXTRATOR_DB_SILENT=1"
    if not exist "%~dp0logs" mkdir "%~dp0logs" >nul 2>&1
    call "%~dp0database\executar_database.bat" > "%~dp0logs\database_startup.log" 2>&1
    set "EXTRATOR_DB_SILENT="
    if errorlevel 1 (
        echo [AVISO] Pipeline de banco retornou erro. Veja logs\database_startup.log
        timeout /t 3 /nobreak >nul 2>&1
    )
)
set "STARTUP_READY=1"
exit /b 0

:check_sqlite_auth_db
if defined EXTRATOR_SECURITY_DB_PATH (
    set "SQLITE_AUTH_DB=!EXTRATOR_SECURITY_DB_PATH!"
) else if defined ProgramData (
    set "SQLITE_AUTH_DB=!ProgramData!\ExtratorESL\security\users.db"
) else (
    set "SQLITE_AUTH_DB=C:\ProgramData\ExtratorESL\security\users.db"
)

if exist "!SQLITE_AUTH_DB!" (
    echo.
    echo [OK] Banco de autenticacao SQLite: !SQLITE_AUTH_DB!
    exit /b 0
)

echo.
echo ================================================================
echo   ERRO: Banco de autenticacao SQLite nao encontrado.
echo.
echo   Caminho verificado: !SQLITE_AUTH_DB!
echo.
echo   Para inicializar o banco de autenticacao, execute:
echo     java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --auth-bootstrap
echo.
echo   Para usar caminho customizado, defina a variavel de ambiente:
echo     EXTRATOR_SECURITY_DB_PATH=C:\caminho\customizado\users.db
echo ================================================================
echo.
pause
exit /b 1

:READ_MENU_OPTION
set "OP="
if /i "%EXTRATOR_MENU_STDIN%"=="1" goto :READ_MENU_OPTION_STDIN

choice /c 123456789U0 /n /m "Escolha uma opcao [1-9, U=Usuarios, 0=Sair]: "
if errorlevel 11 set "OP=0" & exit /b 0
if errorlevel 10 set "OP=10" & exit /b 0
if errorlevel 9 set "OP=9" & exit /b 0
if errorlevel 8 set "OP=8" & exit /b 0
if errorlevel 7 set "OP=7" & exit /b 0
if errorlevel 6 set "OP=6" & exit /b 0
if errorlevel 5 set "OP=5" & exit /b 0
if errorlevel 4 set "OP=4" & exit /b 0
if errorlevel 3 set "OP=3" & exit /b 0
if errorlevel 2 set "OP=2" & exit /b 0
if errorlevel 1 set "OP=1" & exit /b 0
exit /b 1

:READ_MENU_OPTION_STDIN
set /p "OP=Escolha uma opcao: " || exit /b 1
set "OP=%OP: =%"
if not defined OP exit /b 2
if "%OP%"=="00" set "OP=0"
if "%OP:~0,1%"=="0" if not "%OP%"=="0" set "OP=%OP:~1%"
if /i "%OP%"=="U" set "OP=10"
exit /b 0

:AUTH_CHECK
if /i "%EXTRATOR_SKIP_AUTH_CHECK%"=="1" exit /b 0
echo.
echo Autenticacao obrigatoria para executar esta acao.
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --auth-check %~1 "%~2"
if errorlevel 1 (
    echo Acesso negado.
    exit /b 1
)
exit /b 0

:check_jar
if exist "%JAR_PATH%" exit /b 0
echo.
echo ERRO: Arquivo "%JAR_PATH%" nao encontrado.
echo Modo producao exige JAR precompilado.
echo Gere o JAR em outra maquina (build) e copie para a pasta target\.
echo.
pause
exit /b 1

:ensure_java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        goto :eof
    )
)
for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-*" 2^>nul') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
goto :eof

:END
popd
endlocal
exit /b 0
