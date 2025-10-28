@echo off
title Testar API Especifica (rest | graphql | dataexport)
@REM ================================================================
@REM Script: 02-testar_api_especifica.bat
@REM Finalidade:
@REM   Executa testes da API especifica informada como parametro.
@REM   Valores aceitos: 'rest', 'graphql' ou 'dataexport'.
@REM
@REM Uso:
@REM   02-testar_api_especifica.bat <api>
@REM
@REM Parametros:
@REM   <api>  Nome da API a testar: rest | graphql | dataexport
@REM
@REM Exemplos:
@REM   02-testar_api_especifica.bat rest
@REM   02-testar_api_especifica.bat graphql
@REM   02-testar_api_especifica.bat dataexport
@REM ================================================================
setlocal
cd /d "%~dp0"

@REM Valida se o parametro foi fornecido
if "%~1"=="" goto :USAGE

set API=%~1

@REM Valida se o parametro e um dos valores aceitos
if /I "%API%"=="rest" goto :RUN
if /I "%API%"=="graphql" goto :RUN
if /I "%API%"=="dataexport" goto :RUN

echo [ERRO] Parametro invalido: "%API%".
goto :USAGE

:RUN
@REM Executa o comando Java com o parametro da API
java -jar "target\extrator.jar" --testar-api "%API%"
goto :END

:USAGE
@REM Bloco de ajuda
echo.
echo [USO] 02-testar_api_especifica.bat ^<api^>
echo       Onde ^<api^> pode ser: rest ^| graphql ^| dataexport
echo.
echo [EXEMPLOS]
echo   02-testar_api_especifica.bat rest
echo   02-testar_api_especifica.bat graphql
echo   02-testar_api_especifica.bat dataexport

:END
pause