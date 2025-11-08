@echo off

REM ================================================================
REM Script: 05-compilar_projeto.bat
REM Finalidade:
REM   Compila o projeto Maven e gera o JAR executavel.
REM   Deve ser executado apos modificacoes no codigo.
REM
REM Uso:
REM   05-compilar_projeto.bat
REM
REM Funcionalidades:
REM   - Configura JAVA_HOME automaticamente
REM   - Limpa compilacoes anteriores
REM   - Compila o codigo fonte
REM   - Gera o JAR executavel
REM   - Pula testes para compilacao rapida
REM ================================================================

echo ================================================================
echo COMPILANDO PROJETO MAVEN
echo ================================================================

REM Configurar JAVA_HOME automaticamente
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "pom.xml" (
    echo ERRO: Arquivo pom.xml nao encontrado!
    echo Certifique-se de estar no diretorio raiz do projeto.
    echo.
    pause
    exit /b 1
)

echo Java configurado: %JAVA_HOME%
echo.
echo Executando: mvn clean package -DskipTests
echo.
echo ATENCAO: Este processo pode demorar alguns minutos...
echo.

mvn clean package -DskipTests

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo COMPILACAO CONCLUIDA COM SUCESSO!
    echo ================================================================
    echo JAR gerado: target\extrator.jar
) else (
    echo.
    echo ================================================================
    echo COMPILACAO FALHOU ^(Exit Code: %ERRORLEVEL%^)
    echo ================================================================
    echo Verifique os erros acima e corrija o codigo.
)

echo.
pause