@echo off
setlocal EnableExtensions EnableDelayedExpansion

chcp 65001 >nul
pushd "%~dp0"

set "PROD_MODE=1"
set "JAR_PATH=%~dp0target\extrator.jar"

call :ensure_java

:MENU
cls
echo ================================================================
echo            MENU DE PRODUCAO - EXTRATOR ESL CLOUD
echo            suporte: lucasmac.dev@gmail.com
echo            by: @valentelucass
echo ================================================================
echo.
echo  1. Extracao completa (ultimas 24h, com escolha de Faturas GraphQL)
echo  2. Loop de extracao 30 minutos ^(segundo plano^)
echo  3. Extracao por intervalo (com escolha de Faturas GraphQL)
echo  4. Testar API especifica
echo  5. Validar configuracoes
echo  6. Relatorio completo de validacao
echo  7. Exportar CSV
echo  8. Auditar estrutura das APIs
echo  9. Ver ajuda de comandos
echo 10. Gerenciar usuarios de acesso
echo  0. Sair
echo.
set /p "OP=Escolha uma opcao: "

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

echo.
echo Opcao invalida.
timeout /t 2 >nul
goto :MENU

:RUN_01
call :check_jar
call "%~dp001-executar_extracao_completa.bat"
goto :MENU

:RUN_02
call :check_jar
call "%~dp002-testar_api_especifica.bat"
goto :MENU

:RUN_03
call :check_jar
call "%~dp003-validar_config.bat"
goto :MENU

:RUN_04
call :check_jar
call "%~dp004-extracao_por_intervalo.bat"
goto :MENU

:RUN_05
call :check_jar
call "%~dp005-loop_extracao_30min.bat"
goto :MENU

:RUN_06
call :check_jar
call "%~dp006-relatorio-completo-validacao.bat"
goto :MENU

:RUN_07
call :check_jar
call "%~dp007-exportar_csv.bat"
goto :MENU

:RUN_08
call :check_jar
call "%~dp008-auditar_api.bat"
goto :MENU

:RUN_09
call :check_jar
call "%~dp009-gerenciar_usuarios.bat"
goto :MENU

:RUN_AJUDA
call :check_jar
echo.
echo Autenticacao obrigatoria para visualizar ajuda.
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --auth-check RUN_AJUDA "Visualizar ajuda"
if errorlevel 1 (
    echo Acesso negado.
    timeout /t 2 >nul
    goto :MENU
)
echo Executando: java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --ajuda
java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --ajuda
echo.
pause
goto :MENU

:TRY_EXIT
if exist "%JAR_PATH%" (
    echo.
    echo Autenticacao obrigatoria para sair do menu.
    java --enable-native-access=ALL-UNNAMED -jar "%JAR_PATH%" --auth-check MENU_EXIT "Sair do menu principal"
    if errorlevel 1 (
        echo Acesso negado.
        timeout /t 2 >nul
        goto :MENU
    )
)
goto :END

:check_jar
if exist "%JAR_PATH%" goto :eof
echo.
echo ERRO: Arquivo "%JAR_PATH%" nao encontrado.
echo Modo producao exige JAR precompilado.
echo Gere o JAR em outra maquina (build) e copie para a pasta target\.
echo.
pause
goto :END

:ensure_java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        goto :eof
    )
)
for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)
goto :eof

:END
popd
endlocal
exit /b 0
