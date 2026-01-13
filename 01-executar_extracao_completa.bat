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

call "%~dp0mvn.bat" -DskipTests clean package
if errorlevel 1 (
    echo ERRO: Compilacao falhou
    echo.
    pause
    exit /b 1
)

if not exist "target\extrator.jar" (
    echo ERRO: Arquivo target\extrator.jar nao encontrado!
    echo Execute primeiro: mvn clean package -DskipTests
    echo.
    pause
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

REM Carregar variaveis de ambiente do usuario
echo Carregando variaveis de ambiente do usuario...
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('DB_URL', 'User')"') do set "DB_URL=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('DB_USER', 'User')"') do set "DB_USER=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')"') do set "DB_PASSWORD=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('API_BASEURL', 'User')"') do set "API_BASEURL=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('API_REST_TOKEN', 'User')"') do set "API_REST_TOKEN=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('API_GRAPHQL_TOKEN', 'User')"') do set "API_GRAPHQL_TOKEN=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('API_DATAEXPORT_TOKEN', 'User')"') do set "API_DATAEXPORT_TOKEN=%%A"

REM Verificar se as variaveis obrigatorias estao configuradas
if not defined DB_URL (
    echo ERRO: Variavel de ambiente DB_URL nao encontrada!
    echo Configure esta variavel nas configuracoes do sistema.
    echo.
    pause
    exit /b 1
)

if not defined DB_USER (
    echo ERRO: Variavel de ambiente DB_USER nao encontrada!
    echo Configure esta variavel nas configuracoes do sistema.
    echo.
    pause
    exit /b 1
)

if not defined DB_PASSWORD (
    echo ERRO: Variavel de ambiente DB_PASSWORD nao encontrada!
    echo Configure esta variavel nas configuracoes do sistema.
    echo.
    pause
    exit /b 1
)

echo Executando: java -jar "target\extrator.jar" --fluxo-completo
echo.
echo ATENCAO: Este processo pode demorar varios minutos...
echo.

java -jar "target\extrator.jar" --fluxo-completo

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
