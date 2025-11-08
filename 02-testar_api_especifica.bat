@echo off
setlocal

REM ================================================================
REM Script: 02-testar_api_especifica.bat
REM Finalidade:
REM   Executa testes da API especifica informada como parametro.
REM   Valores aceitos: 'rest', 'graphql' ou 'dataexport'.
REM
REM Uso:
REM   02-testar_api_especifica.bat <api>
REM
REM Parametros:
REM   %%1  Nome da API a testar: rest | graphql | dataexport
REM
REM Exemplos:
REM   02-testar_api_especifica.bat rest
REM   02-testar_api_especifica.bat graphql
REM   02-testar_api_especifica.bat dataexport
REM ================================================================

if "%~1"=="" (
    echo ERRO: Parametro obrigatorio nao informado!
    echo.
    echo Uso: %~nx0 ^<api^>
    echo   api: rest ^| graphql ^| dataexport
    echo.
    echo Exemplos:
    echo   %~nx0 rest
    echo   %~nx0 graphql
    echo   %~nx0 dataexport
    echo.
    pause
    exit /b 1
)

set "API=%~1"

if /i not "%API%"=="rest" if /i not "%API%"=="graphql" if /i not "%API%"=="dataexport" (
    echo ERRO: API '%API%' nao reconhecida!
    echo.
    echo APIs suportadas: rest, graphql, dataexport
    echo.
    pause
    exit /b 1
)

echo ================================================================
echo TESTANDO API: %API%
echo ================================================================

if not exist "target\extrator.jar" (
    echo ERRO: Arquivo target\extrator.jar nao encontrado!
    echo Execute primeiro: mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

echo Executando: java -jar "target\extrator.jar" --testar-api %API%
echo.

java -jar "target\extrator.jar" --testar-api %API%

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo TESTE CONCLUIDO COM SUCESSO!
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo TESTE FALHOU ^(Exit Code: %ERRORLEVEL%^)
    echo ================================================================
)

echo.
pause