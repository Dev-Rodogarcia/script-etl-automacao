@echo off
setlocal

echo ================================================================
echo CONSOLE DE LOOP DE EXTRACAO ^(30 minutos^)
echo ================================================================

call "%~dp0mvn.bat" -q -DskipTests clean package
if errorlevel 1 (
  echo ERRO: Compilacao falhou
  exit /b 1
)

if not exist "%~dp0target\extrator.jar" (
  echo ERRO: target\extrator.jar nao encontrado
  exit /b 1
)

java -jar "%~dp0target\extrator.jar" --loop

endlocal
exit /b 0
