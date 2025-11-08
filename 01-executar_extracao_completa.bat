@echo off
setlocal

REM ================================================================
REM Script: 01-executar_extracao_completa.bat
REM Finalidade:
REM   Executa a extracao completa de dados de todas as APIs.
REM   Este e o script principal para execucao automatizada.
REM
REM Uso:
REM   01-executar_extracao_completa.bat
REM
REM Funcionalidades:
REM   - Executa extracao de todas as entidades
REM   - Gera logs detalhados
REM   - Salva dados no banco configurado
REM ================================================================

echo ================================================================
echo INICIANDO EXTRACAO COMPLETA DE DADOS
echo ================================================================

if not exist "target\extrator.jar" (
    echo ERRO: Arquivo target\extrator.jar nao encontrado!
    echo Execute primeiro: mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

echo Executando: java -jar "target\extrator.jar"
echo.
echo ATENCAO: Este processo pode demorar varios minutos...
echo.

java -jar "target\extrator.jar"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo EXTRACAO COMPLETA CONCLUIDA COM SUCESSO!
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo EXTRACAO FALHOU ^(Exit Code: %ERRORLEVEL%^)
    echo ================================================================
)

echo.
echo Verifique os logs na pasta 'logs' para mais detalhes.
echo.
pause