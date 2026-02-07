@echo off
setlocal

echo ================================================================
echo CONSOLE DE LOOP DE EXTRACAO ^(30 minutos^)
echo ================================================================

pushd "%~dp0"

if /i "%PROD_MODE%"=="1" (
  echo Modo producao: pulando compilacao.
) else (
  call "%~dp0mvn.bat" -q -DskipTests clean package
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

REM Configurar JAVA_HOME automaticamente (Java 17+)
if not defined JAVA_HOME (
  REM Tenta encontrar JDK 17+ no Eclipse Adoptium
  for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    goto :javahomefound
  )
  REM Se nao encontrar, tenta qualquer JDK 17+ no Adoptium
  for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-*" 2^>nul ^| sort /r') do (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    goto :javahomefound
  )
)
:javahomefound
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
  ) else (
    set "JAVA_HOME="
  )
)

java -jar "%~dp0target\extrator.jar" --loop

popd

endlocal
exit /b 0
