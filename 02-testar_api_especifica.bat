@echo off
setlocal

REM Ajusta code page para evitar erros de parsing com acentos e parenteses
chcp 1252 >nul

REM ================================================================
REM Script: 02-testar_api_especifica.bat
REM Finalidade:
REM   Executa testes da API especifica informada como parametro.
REM   Valores aceitos: 'graphql' ou 'dataexport'.
REM
REM Uso:
REM   02-testar_api_especifica.bat <api> [entidade]
REM
REM Parametros:
REM   %1  Nome da API a testar: graphql | dataexport
REM   %2  Entidade (opcional):
REM       GraphQL -> coletas | fretes | faturas_graphql
REM       DataExport -> manifestos | cotacoes | localizacao_carga | contas_a_pagar | faturas_por_cliente
REM
REM Exemplos:
REM   02-testar_api_especifica.bat graphql
REM   02-testar_api_especifica.bat graphql fretes
REM   02-testar_api_especifica.bat graphql coletas
REM   02-testar_api_especifica.bat dataexport
REM   02-testar_api_especifica.bat dataexport manifestos
REM   02-testar_api_especifica.bat dataexport cotacoes
REM   02-testar_api_especifica.bat dataexport localizacao_carga
REM   02-testar_api_especifica.bat dataexport contas_a_pagar
REM   02-testar_api_especifica.bat dataexport faturas_por_cliente
REM ================================================================

if "%~1"=="" goto :USAGE

set "API=%~1"
set "ENTIDADE=%~2"

REM Valida API e entidade usando ramos sem parenteses
if /i "%API%"=="graphql" goto :CHECK_GRAPHQL
if /i "%API%"=="dataexport" goto :CHECK_DATAEXPORT
echo ERRO: API '%API%' nao reconhecida!
echo.
echo APIs suportadas: graphql, dataexport
echo.
pause
exit /b 1

:CHECK_GRAPHQL
if "%ENTIDADE%"=="" goto :RUN
if /i "%ENTIDADE%"=="coletas" goto :RUN
if /i "%ENTIDADE%"=="fretes" goto :RUN
if /i "%ENTIDADE%"=="faturas_graphql" goto :RUN
if /i "%ENTIDADE%"=="faturas" goto :RUN
echo ERRO: Entidade '%ENTIDADE%' invalida para API GraphQL!
echo.
echo Entidades suportadas: coletas, fretes, faturas_graphql, faturas
echo.
pause
exit /b 1

:CHECK_DATAEXPORT
if "%ENTIDADE%"=="" goto :RUN
if /i "%ENTIDADE%"=="manifestos" goto :RUN
if /i "%ENTIDADE%"=="cotacoes" goto :RUN
if /i "%ENTIDADE%"=="localizacao_carga" goto :RUN
if /i "%ENTIDADE%"=="localizacao_de_carga" goto :RUN
if /i "%ENTIDADE%"=="contas_a_pagar" goto :RUN
if /i "%ENTIDADE%"=="contasapagar" goto :RUN
if /i "%ENTIDADE%"=="faturas_por_cliente" goto :RUN
if /i "%ENTIDADE%"=="faturasporcliente" goto :RUN
echo ERRO: Entidade '%ENTIDADE%' invalida para API DataExport!
echo.
echo Entidades suportadas: manifestos, cotacoes, localizacao_carga, contas_a_pagar, faturas_por_cliente
echo.
pause
exit /b 1

:USAGE
echo ERRO: Parametro obrigatorio nao informado!
echo.
echo Uso: %~nx0 ^<api^> [entidade]
echo   api: graphql ^| dataexport
echo   entidade (opcional):
echo     graphql: coletas ^| fretes ^| faturas_graphql ^| faturas
echo     dataexport: manifestos ^| cotacoes ^| localizacao_carga ^| contas_a_pagar ^| faturas_por_cliente
echo.
echo Exemplos:
echo   %~nx0 graphql
echo   %~nx0 graphql fretes
echo   %~nx0 graphql faturas_graphql
echo   %~nx0 graphql faturas
echo   %~nx0 dataexport
echo   %~nx0 dataexport manifestos
echo.
pause
exit /b 1

:RUN
echo ================================================================
echo TESTANDO API: %API%
echo ================================================================

if /i "%API%"=="graphql" (
  if /i "%ENTIDADE%"=="faturas" set "API_GRAPHQL_FATURAS_DIAS_JANELA=1"
  if /i "%ENTIDADE%"=="faturas" set "API_GRAPHQL_FATURAS_MAX_PAGINAS=20"
  if /i "%ENTIDADE%"=="faturas_graphql" set "API_GRAPHQL_FATURAS_DIAS_JANELA=1"
  if /i "%ENTIDADE%"=="faturas_graphql" set "API_GRAPHQL_FATURAS_MAX_PAGINAS=20"
)

call "%~dp0mvn.bat" -q -DskipTests clean compile
if errorlevel 1 goto :COMPILE_FAIL

if not exist "target\classes" goto :NOJAR

set "CMD_ARGS=--testar-api %API%"
if not "%ENTIDADE%"=="" set "CMD_ARGS=%CMD_ARGS% %ENTIDADE%"
echo Executando: mvn exec:java -Dexec.mainClass=br.com.extrator.Main -Dexec.args="%CMD_ARGS%" 
echo.

call "%~dp0mvn.bat" -q -DskipTests exec:java -Dexec.mainClass=br.com.extrator.Main -Dexec.args="%CMD_ARGS%"
if errorlevel 1 goto :FAIL
goto :SUCCESS

:NOJAR
echo ERRO: Arquivo target\extrator.jar nao encontrado!
echo Execute primeiro: mvn package -DskipTests -q
echo.
pause
exit /b 1

:SUCCESS
echo.
echo ================================================================
echo TESTE CONCLUIDO COM SUCESSO!
echo ================================================================
goto :END

:FAIL
echo.
echo ================================================================
echo TESTE FALHOU (Exit Code: %ERRORLEVEL%)
echo ================================================================
goto :END

:COMPILE_FAIL
echo ERRO: Compilacao falhou
echo.
pause
exit /b 1

:END
echo.
pause
