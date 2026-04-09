@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul
pushd "%~dp0"

set "SCRIPT_ROOT=%~dp0"
for %%I in ("%~dp0..\..") do set "REPO_ROOT=%%~fI"
set "JAR_PATH=%REPO_ROOT%\target\extrator.jar"
set "DB_STARTUP_LOG=%REPO_ROOT%\logs\aplicacao\operacoes\database_startup.log"
set "BASICS_READY=0"
set "STARTUP_READY=0"
set "SQLITE_AUTH_DB="

REM ----------------------------------------------------------------
REM PRODUCAO: apenas executa artefato ja gerado.
REM Nao compila, nao testa, nao empacota.
REM Para compilar, use o ambiente de desenvolvimento separadamente.
REM ----------------------------------------------------------------
set "PROD_MODE=1"

if /i "%~1"=="--auto-intervalo" goto :RUN_AUTO_INTERVALO
if /i "%~1"=="--auto-extracao-completa" (
    call :PREPARE_SECURITY
    if errorlevel 1 (
        set "AUTO_EXIT=1"
        goto :END_WITH_CODE
    )
    call :PREPARE_DATABASE
    if errorlevel 1 (
        set "AUTO_EXIT=1"
        goto :END_WITH_CODE
    )
    call :RUN_SCRIPT_AUTHORIZED "01-executar_extracao_completa.bat" "%~2"
    set "AUTO_EXIT=!ERRORLEVEL!"
    goto :END_WITH_CODE
)

:MENU
cls
echo ================================================================
echo            MENU DE PRODUCAO - EXTRATOR ESL CLOUD
echo            suporte: lucasmac.dev@gmail.com
echo            by: @valentelucass
echo ================================================================
echo.
echo 01. Extracao completa ultimas 24h ^(inclui inventario e sinistros^)
echo 02. Loop de extracao 30 minutos ^(inclui inventario e sinistros^)
echo 03. Extracao por intervalo ^(inclui inventario e sinistros^)
echo 04. Testar API especifica ^(inventario e sinistros disponiveis^)
echo 05. Validar configuracoes
echo 06. Bateria extrema e relatorio de saude do ETL
echo 07. Exportar CSV
echo 08. Auditar estrutura das APIs
echo 09. Ver ajuda de comandos
echo 10. Gerenciar usuarios de acesso ^(tecla U^)
echo 00. Sair
echo.
echo Cobertura atual do ETL:
echo   GraphQL   = coletas, fretes, faturas_graphql, usuarios_sistema
echo   DataExport = manifestos, cotacoes, localizacao_cargas, contas_a_pagar, faturas_por_cliente, inventario, sinistros
echo.
if not "%BASICS_READY%"=="1" (
    echo Ambiente sera validado ao executar a primeira opcao.
    echo.
) else if not "%STARTUP_READY%"=="1" (
    echo Banco e objetos operacionais serao preparados apos a autenticacao da primeira acao que exigir escrita.
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

:RUN_AUTO_INTERVALO
call :PREPARE_SECURITY
if errorlevel 1 (
    set "AUTO_EXIT=1"
    goto :END_WITH_CODE
)
call :PREPARE_DATABASE
if errorlevel 1 (
    set "AUTO_EXIT=1"
    goto :END_WITH_CODE
)
if "%~2"=="" (
    echo.
    echo ERRO: Data de inicio nao informada para o modo automatico.
    echo Uso: 00-PRODUCAO_START.bat --auto-intervalo YYYY-MM-DD YYYY-MM-DD [api] [entidade] [--sem-faturas-graphql^|--com-faturas-graphql]
    echo Exemplo DataExport: 00-PRODUCAO_START.bat --auto-intervalo 2026-04-01 2026-04-02 dataexport inventario
    set "AUTO_EXIT=1"
    goto :END_WITH_CODE
)
if "%~3"=="" (
    echo.
    echo ERRO: Data de fim nao informada para o modo automatico.
    echo Uso: 00-PRODUCAO_START.bat --auto-intervalo YYYY-MM-DD YYYY-MM-DD [api] [entidade] [--sem-faturas-graphql^|--com-faturas-graphql]
    echo Exemplo DataExport: 00-PRODUCAO_START.bat --auto-intervalo 2026-04-01 2026-04-02 dataexport sinistros
    set "AUTO_EXIT=1"
    goto :END_WITH_CODE
)
call "%SCRIPT_ROOT%04-extracao_por_intervalo.bat" "%~2" "%~3" "%~4" "%~5" "%~6"
set "AUTO_EXIT=!ERRORLEVEL!"
goto :END_WITH_CODE

:RUN_01
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_EXTRACAO_COMPLETA "Executar extracao completa"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :PREPARE_DATABASE
if errorlevel 1 goto :MENU
call :RUN_SCRIPT_AUTHORIZED "01-executar_extracao_completa.bat"
goto :MENU

:RUN_02
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_TESTAR_API "Testar API especifica"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :RUN_SCRIPT_AUTHORIZED "02-testar_api_especifica.bat"
goto :MENU

:RUN_03
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_VALIDAR_CONFIG "Validar configuracoes"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :RUN_SCRIPT_AUTHORIZED "03-validar_config.bat"
goto :MENU

:RUN_04
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_EXTRACAO_INTERVALO "Executar extracao por intervalo"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :PREPARE_DATABASE
if errorlevel 1 goto :MENU
call :RUN_SCRIPT_AUTHORIZED "04-extracao_por_intervalo.bat"
goto :MENU

:RUN_05
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :PREPARE_DATABASE
if errorlevel 1 goto :MENU
call :RUN_SCRIPT "05-loop_extracao_30min.bat"
goto :MENU

:RUN_06
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_BATERIA_EXTREMA "Executar bateria extrema e relatorio de saude do ETL"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :PREPARE_DATABASE
if errorlevel 1 goto :MENU
call :RUN_SCRIPT_AUTHORIZED "06-relatorio-completo-validacao.bat"
goto :MENU

:RUN_07
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_EXPORTAR_CSV "Exportar dados para CSV"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :PREPARE_DATABASE
if errorlevel 1 goto :MENU
call :RUN_SCRIPT_AUTHORIZED "07-exportar_csv.bat"
goto :MENU

:RUN_08
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :AUTH_CHECK RUN_AUDITORIA_API "Auditar estrutura das APIs"
if errorlevel 1 (
    timeout /t 2 /nobreak >nul 2>&1
    goto :MENU
)
call :RUN_SCRIPT_AUTHORIZED "08-auditar_api.bat"
goto :MENU

:RUN_09
call :PREPARE_SECURITY
if errorlevel 1 goto :MENU
call :RUN_SCRIPT "09-gerenciar_usuarios.bat"
goto :MENU

:RUN_AJUDA
call :PREPARE_SECURITY
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

:PREPARE_SECURITY
if "%BASICS_READY%"=="1" exit /b 0
call :ensure_java
call :check_jar
if errorlevel 1 exit /b 1

call :check_sqlite_auth_db
if errorlevel 1 exit /b 1

set "BASICS_READY=1"
exit /b 0

:PREPARE_DATABASE
if "%STARTUP_READY%"=="1" exit /b 0

if exist "%REPO_ROOT%\database\executar_database.bat" (
    echo.
    echo Preparando ambiente do banco...
    set "EXTRATOR_DB_SILENT=1"
    if not exist "%REPO_ROOT%\logs\aplicacao\operacoes" mkdir "%REPO_ROOT%\logs\aplicacao\operacoes" >nul 2>&1
    call "%REPO_ROOT%\database\executar_database.bat" > "%DB_STARTUP_LOG%" 2>&1
    set "EXTRATOR_DB_SILENT="
    if errorlevel 1 (
        echo [AVISO] Pipeline de banco retornou erro. Veja logs\aplicacao\operacoes\database_startup.log
        timeout /t 3 /nobreak >nul 2>&1
    ) else (
        echo [OK] Ambiente de banco preparado, incluindo inventario/sinistros e views do BI.
        echo [INFO] Referencia: logs\aplicacao\operacoes\database_startup.log
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
set /p "OP=Escolha uma opcao [1-10, U=Usuarios, 0=Sair]: " || exit /b 1
set "OP=%OP: =%"
if not defined OP exit /b 2
if "%OP%"=="00" set "OP=0"
if "%OP:~0,1%"=="0" if not "%OP%"=="0" set "OP=%OP:~1%"
if /i "%OP%"=="U" set "OP=10"
for %%V in (0 1 2 3 4 5 6 7 8 9 10) do (
    if "%OP%"=="%%V" exit /b 0
)
exit /b 2

:RUN_SCRIPT
set "TARGET_SCRIPT=%~1"
set "TARGET_ARG1=%~2"
if not defined TARGET_SCRIPT exit /b 1
if not exist "%SCRIPT_ROOT%%TARGET_SCRIPT%" (
    echo.
    echo ERRO: Script "%SCRIPT_ROOT%%TARGET_SCRIPT%" nao encontrado.
    echo.
    pause
    exit /b 1
)
if defined TARGET_ARG1 (
    call "%SCRIPT_ROOT%%TARGET_SCRIPT%" "%TARGET_ARG1%"
) else (
    call "%SCRIPT_ROOT%%TARGET_SCRIPT%"
)
set "TARGET_EXIT=!ERRORLEVEL!"
if not "!TARGET_EXIT!"=="0" (
    echo.
    echo [AVISO] %TARGET_SCRIPT% retornou codigo !TARGET_EXIT!.
    timeout /t 2 /nobreak >nul 2>&1
)
exit /b 0

:RUN_SCRIPT_AUTHORIZED
set "TARGET_SCRIPT=%~1"
set "TARGET_ARG1=%~2"
set "PREV_SKIP_AUTH_CHECK=%EXTRATOR_SKIP_AUTH_CHECK%"
set "EXTRATOR_SKIP_AUTH_CHECK=1"
if defined TARGET_ARG1 (
    call :RUN_SCRIPT "%TARGET_SCRIPT%" "%TARGET_ARG1%"
) else (
    call :RUN_SCRIPT "%TARGET_SCRIPT%"
)
set "TARGET_EXIT=!ERRORLEVEL!"
if defined PREV_SKIP_AUTH_CHECK (
    set "EXTRATOR_SKIP_AUTH_CHECK=%PREV_SKIP_AUTH_CHECK%"
) else (
    set "EXTRATOR_SKIP_AUTH_CHECK="
)
exit /b !TARGET_EXIT!

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
for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" 2^>nul') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" 2^>nul') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
goto :eof

:END
popd
endlocal
exit /b 0

:END_WITH_CODE
popd
endlocal & exit /b %AUTO_EXIT%
