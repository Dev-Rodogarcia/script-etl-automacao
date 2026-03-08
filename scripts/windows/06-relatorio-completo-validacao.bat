@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM ==[DOC-FILE]===============================================================
REM Arquivo : 06-relatorio-completo-validacao.bat
REM Tipo    : Script operacional Windows (.bat)
REM Papel   : Executa relatorio completo unificado de validacao e auditoria.
REM
REM Conecta com:
REM - call: %~dp0mvn.bat
REM - call: :AUTH_CHECK
REM - java -jar: target\extrator.jar
REM
REM Fluxo geral:
REM 1) Compila (se necessario) e verifica JAR.
REM 2) Executa as 4 etapas de validacao sequencialmente.
REM 3) Exibe resumo consolidado com total de erros.
REM
REM Variaveis-chave:
REM - JAVA_HOME: controle de estado do script.
REM - VALIDAR_EXIT: exit code da validacao de acesso.
REM - VALIDAR_DADOS_EXIT: exit code da validacao de dados.
REM - AUDITORIA_EXIT: exit code da auditoria.
REM - VALIDAR_MANIFESTOS_EXIT: exit code da validacao de manifestos.
REM - TOTAL_ERROS: contador de etapas que falharam.
REM [DOC-FILE-END]===========================================================

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul

REM ================================================================
REM Script: 06-relatorio-completo-validacao.bat
REM Finalidade:
REM   Executa relatorio completo unificado de validacao e auditoria.
REM   Consolida TODAS as validacoes em um unico relatorio completo.
REM
REM   Este script UNIFICA os antigos:
REM   - 04-executar_auditoria.bat (removido)
REM   - 07-validar-manifestos.bat (removido)
REM   - 10-validar-dados-completo.bat (removido)
REM
REM Uso:
REM   06-relatorio-completo-validacao.bat
REM
REM Funcionalidades:
REM   - Valida configuracoes e conectividade
REM   - Valida completude, gaps, integridade, qualidade e metadata
REM   - Executa auditoria de dados das ultimas 24 horas
REM   - Valida especificamente manifestos (contagem, duplicados, integridade)
REM   - Gera relatorio consolidado no console
REM ================================================================

echo ================================================================
echo
echo     RELATORIO COMPLETO DE VALIDACAO E AUDITORIA
echo     Sistema desenvolvido por @valentelucass
echo
echo ================================================================
echo.
echo Este script executa todas as validacoes e auditorias disponiveis:
echo.
echo   1. Validacao de Acesso (configuracoes e conectividade)
echo   2. Validacao Completa de Dados - Prova dos 9
echo   3. Auditoria de Dados (ultimas 24 horas)
echo   4. Validacao Especifica de Manifestos
echo.
echo ================================================================
echo.

REM Compilar projeto automaticamente (se necessario)
if /i "%PROD_MODE%"=="1" (
    echo Modo producao: pulando compilacao.
) else (
    echo Compilando projeto (se necessario)...
    call "%~dp0mvn.bat" -q -DskipTests package
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
    )
    echo.
    pause
    exit /b 1
)

REM Configurar JAVA_HOME automaticamente (Java 17+)
if not defined JAVA_HOME (
    for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul') do (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
        goto :javahomefound
    )
    for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-*" 2^>nul') do (
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

echo ================================================================
echo.
echo ETAPA 1/4: VALIDACAO DE ACESSO E CONFIGURACOES
echo.
echo ================================================================
echo.

call :AUTH_CHECK RUN_RELATORIO_VALIDACAO "Executar relatorio completo de validacao"
if errorlevel 1 exit /b 1

java --enable-native-access=ALL-UNNAMED -jar "target\extrator.jar" --validar
set "VALIDAR_EXIT=%ERRORLEVEL%"

echo.
echo ================================================================
echo.
echo ETAPA 2/4: VALIDACAO COMPLETA DE DADOS (PROVA DOS 9)
echo.
echo ================================================================
echo.

java --enable-native-access=ALL-UNNAMED -jar "target\extrator.jar" --validar-dados
set "VALIDAR_DADOS_EXIT=%ERRORLEVEL%"

echo.
echo ================================================================
echo.
echo ETAPA 3/4: AUDITORIA DE DADOS (ULTIMAS 24 HORAS)
echo.
echo ================================================================
echo.

java --enable-native-access=ALL-UNNAMED -jar "target\extrator.jar" --auditoria
set "AUDITORIA_EXIT=%ERRORLEVEL%"

echo.
echo ================================================================
echo.
echo ETAPA 4/4: VALIDACAO ESPECIFICA DE MANIFESTOS
echo.
echo ================================================================
echo.

java --enable-native-access=ALL-UNNAMED -jar "target\extrator.jar" --validar-manifestos
set "VALIDAR_MANIFESTOS_EXIT=%ERRORLEVEL%"

echo.
echo ================================================================
echo
echo     RESUMO DO RELATORIO COMPLETO
echo
echo ================================================================
echo.

set "TOTAL_ERROS=0"

if !VALIDAR_EXIT! equ 0 (
    echo   [OK] Validacao de Acesso: SUCESSO
) else (
    echo   [ERRO] Validacao de Acesso: FALHOU ^(Exit Code: !VALIDAR_EXIT!^)
    set /a TOTAL_ERROS+=1
)

if !VALIDAR_DADOS_EXIT! equ 0 (
    echo   [OK] Validacao Completa de Dados: SUCESSO
) else (
    echo   [ERRO] Validacao Completa de Dados: FALHOU ^(Exit Code: !VALIDAR_DADOS_EXIT!^)
    set /a TOTAL_ERROS+=1
)

if !AUDITORIA_EXIT! equ 0 (
    echo   [OK] Auditoria de Dados: SUCESSO
) else (
    echo   [ERRO] Auditoria de Dados: FALHOU ^(Exit Code: !AUDITORIA_EXIT!^)
    set /a TOTAL_ERROS+=1
)

if !VALIDAR_MANIFESTOS_EXIT! equ 0 (
    echo   [OK] Validacao de Manifestos: SUCESSO
) else (
    echo   [ERRO] Validacao de Manifestos: FALHOU ^(Exit Code: !VALIDAR_MANIFESTOS_EXIT!^)
    set /a TOTAL_ERROS+=1
)

echo.
echo ================================================================
echo.

if !TOTAL_ERROS! equ 0 (
    echo   TODAS AS VALIDACOES FORAM CONCLUIDAS COM SUCESSO!
    echo.
    echo   O sistema esta integro e funcionando corretamente.
    echo.
) else (
    echo   ATENCAO: !TOTAL_ERROS! validacao(oes) falharam.
    echo.
    echo   Revise os resultados acima para identificar os problemas.
    echo   Verifique os logs na pasta 'logs' para mais detalhes.
    echo.
)

echo ================================================================
echo.
echo Relatorio completo gerado por @valentelucass
echo lucasmac.dev@gmail.com
echo.
echo ================================================================
echo.

pause
endlocal
exit /b 0

:AUTH_CHECK
if /i "%EXTRATOR_SKIP_AUTH_CHECK%"=="1" exit /b 0
echo.
echo Autenticacao obrigatoria para executar esta acao.
java --enable-native-access=ALL-UNNAMED -jar "target\extrator.jar" --auth-check %~1 "%~2"
if errorlevel 1 (
    echo Acesso negado.
    echo.
    pause
    exit /b 1
)
exit /b 0
