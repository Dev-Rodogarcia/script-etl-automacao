@echo off

REM ================================================================
REM Script: 07-validar-manifestos.bat
REM Finalidade:
REM   Executa validação Java para verificar contagem de manifestos
REM   Compara registros extraídos vs salvos no banco
REM
REM Uso:
REM   07-validar-manifestos.bat
REM ================================================================

echo ================================================================
echo VALIDACAO DE MANIFESTOS
echo ================================================================
echo.

REM Configurar JAVA_HOME automaticamente
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "target\extrator.jar" (
    echo ERRO: Arquivo target\extrator.jar nao encontrado!
    echo Certifique-se de compilar o projeto primeiro.
    echo Execute: 05-compilar_projeto.bat
    echo.
    pause
    exit /b 1
)

echo Executando validacao de manifestos...
echo.

java -jar target\extrator.jar --validar-manifestos

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo VALIDACAO CONCLUIDA
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo VALIDACAO FALHOU ^(Exit Code: %ERRORLEVEL%^)
    echo ================================================================
)

echo.
pause

