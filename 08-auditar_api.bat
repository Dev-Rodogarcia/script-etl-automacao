@echo off
setlocal enableextensions
chcp 65001 >nul

echo ================================================================
echo AUDITAR ESTRUTURA DA API
echo ================================================================
echo Compilando projeto...
call mvn -Dmaven.clean.skip=true package -DskipTests
if errorlevel 1 (
  echo Falha na compilacao. Abortando.
  exit /b 1
)

echo Executando auditor...
set CP=target\extrator.jar
java -cp "%CP%" br.com.extrator.auditoria.validacao.AuditorEstruturaApi
set EXITCODE=%ERRORLEVEL%

if %EXITCODE% NEQ 0 (
  echo Auditoria concluiu com erro (%EXITCODE%).
) else (
  echo Auditoria concluida com sucesso.
)

echo Saida em pasta 'relatorios'.
pause
endlocal
