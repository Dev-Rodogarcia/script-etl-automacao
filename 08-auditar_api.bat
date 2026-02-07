@echo off
setlocal enableextensions
chcp 65001 >nul

REM ================================================================
REM Script: 08-auditar_api.bat
REM Finalidade:
REM   Audita a estrutura das APIs (GraphQL e DataExport).
REM   Gera relatorio CSV com todos os campos disponiveis em cada entidade.
REM
REM Uso:
REM   08-auditar_api.bat
REM
REM Funcionalidades:
REM   - Compila o projeto automaticamente
REM   - Executa auditoria de estrutura das APIs
REM   - Gera relatorio CSV na pasta 'relatorios'
REM ================================================================

echo ================================================================
echo AUDITAR ESTRUTURA DA API
echo ================================================================
echo.

if /i "%PROD_MODE%"=="1" (
    echo Modo producao: pulando compilacao.
) else (
    echo Compilando projeto...
    call "%~dp0mvn.bat" -q -DskipTests clean package
    if errorlevel 1 (
        echo ERRO: Compilacao falhou
        echo.
        pause
        exit /b 1
    )
)

if not exist "target\extrator.jar" (
    echo ERRO: Arquivo target\extrator.jar nao encontrado!
    if /i "%PROD_MODE%"=="1" (
        echo Modo producao requer JAR precompilado.
    ) else (
        echo Execute primeiro: mvn clean package -DskipTests
    )
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
        echo AVISO: JAVA_HOME configurado, mas java.exe nao encontrado
    )
)

echo Executando auditor...
echo.

REM AuditorEstruturaApi tem seu proprio main, executar diretamente via JAR
java -cp "target\extrator.jar" br.com.extrator.auditoria.validacao.AuditorEstruturaApi
set EXITCODE=%ERRORLEVEL%

if %EXITCODE% NEQ 0 (
    echo.
    echo ================================================================
    echo AUDITORIA CONCLUIDA COM ERRO (Exit Code: %EXITCODE%)
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo AUDITORIA CONCLUIDA COM SUCESSO!
    echo ================================================================
)

echo.
echo Saida em pasta 'relatorios'.
echo.
pause
endlocal
