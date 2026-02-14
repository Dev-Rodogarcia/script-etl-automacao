@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

echo ================================================================
echo GERENCIAR LOOP DE EXTRACAO ^(30 minutos - segundo plano^)
echo ================================================================
echo.

pushd "%~dp0"

if /i "%PROD_MODE%"=="1" (
  echo Modo producao: pulando compilacao.
) else (
  call "%~dp0mvn.bat" -q -DskipTests package
  if errorlevel 1 (
    echo ERRO: Compilacao falhou
    popd
    exit /b 1
  )
)

if not exist "%~dp0target\extrator.jar" (
  echo ERRO: target\extrator.jar nao encontrado
  if /i "%PROD_MODE%"=="1" (
    echo Modo producao requer JAR precompilado.
  )
  popd
  exit /b 1
)

if not defined JAVA_HOME (
  for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    goto :javahomefound
  )
  for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    goto :javahomefound
  )
)
:javahomefound
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
  )
)

:MENU
echo.
echo ================================================================
echo MENU LOOP EM SEGUNDO PLANO
echo ================================================================
echo  1. Iniciar loop daemon
echo  2. Status do loop daemon
echo  3. Parar loop daemon
echo  0. Voltar
echo.
set /p OP="Escolha uma opcao: "

if "%OP%"=="1" goto :START
if "%OP%"=="2" goto :STATUS
if "%OP%"=="3" goto :STOP
if "%OP%"=="0" goto :EXIT_LOOP_MENU

echo Opcao invalida.
timeout /t 2 >nul
goto :MENU

:START
call :AUTH_CHECK LOOP_START "Iniciar loop daemon"
if errorlevel 1 goto :MENU
java --enable-native-access=ALL-UNNAMED -jar "%~dp0target\extrator.jar" --loop-daemon-start
echo.
pause
goto :MENU

:STATUS
call :AUTH_CHECK LOOP_STATUS "Consultar status do loop daemon"
if errorlevel 1 goto :MENU
java --enable-native-access=ALL-UNNAMED -jar "%~dp0target\extrator.jar" --loop-daemon-status
echo.
pause
goto :MENU

:STOP
call :AUTH_CHECK LOOP_STOP "Parar loop daemon"
if errorlevel 1 goto :MENU
java --enable-native-access=ALL-UNNAMED -jar "%~dp0target\extrator.jar" --loop-daemon-stop
echo.
pause
goto :MENU

:EXIT_LOOP_MENU
call :AUTH_CHECK LOOP_EXIT_MENU "Sair do menu de loop"
if errorlevel 1 goto :MENU
goto :END

:AUTH_CHECK
if /i "%EXTRATOR_SKIP_AUTH_CHECK%"=="1" exit /b 0
echo.
echo Autenticacao obrigatoria para executar esta acao.
java --enable-native-access=ALL-UNNAMED -jar "%~dp0target\extrator.jar" --auth-check %~1 "%~2"
if errorlevel 1 (
  echo Acesso negado.
  echo.
  pause
  exit /b 1
)
exit /b 0

:END
popd
endlocal
exit /b 0
