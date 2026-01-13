@echo off
setlocal

REM ================================================================
REM Script: 06-relatorio-completo-validacao.bat
REM Finalidade:
REM   Executa relatório completo unificado de validação e auditoria.
REM   Consolida TODAS as validações em um único relatório completo.
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
REM   - Valida configurações e conectividade
REM   - Valida completude, gaps, integridade, qualidade e metadata
REM   - Executa auditoria de dados das últimas 24 horas
REM   - Valida especificamente manifestos (contagem, duplicados, integridade)
REM   - Gera relatório consolidado no console
REM ================================================================

echo ╔══════════════════════════════════════════════════════════════════════════════╗
echo ║                                                                              ║
echo ║           📊 RELATÓRIO COMPLETO DE VALIDAÇÃO E AUDITORIA                     ║
echo ║           Sistema desenvolvido por @valentelucass                            ║
echo ║                                                                              ║
echo ╚══════════════════════════════════════════════════════════════════════════════╝
echo.
echo Este script executa todas as validações e auditorias disponíveis:
echo.
echo   1. ✓ Validação de Acesso (configurações e conectividade)
echo   2. ✓ Validação Completa de Dados - Prova dos 9
echo   3. ✓ Auditoria de Dados (últimas 24 horas)
echo   4. ✓ Validação Específica de Manifestos
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

REM Compilar projeto automaticamente (se necessário)
echo Compilando projeto (se necessário)...
call "%~dp0mvn.bat" -q -DskipTests package
if errorlevel 1 (
    echo ❌ ERRO: Compilação falhou
    echo.
    pause
    exit /b 1
)

if not exist "target\extrator.jar" (
    echo ❌ ERRO: Arquivo target\extrator.jar não encontrado após compilação!
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
    REM Se não encontrar, tenta qualquer JDK 17+ no Adoptium
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
        echo ⚠️  JAVA_HOME configurado, mas java.exe não encontrado
    )
)

echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo 📋 ETAPA 1/4: VALIDAÇÃO DE ACESSO E CONFIGURAÇÕES
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

java -jar "target\extrator.jar" --validar
set VALIDAR_EXIT=%ERRORLEVEL%

echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo 📋 ETAPA 2/4: VALIDAÇÃO COMPLETA DE DADOS (PROVA DOS 9)
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

java -jar "target\extrator.jar" --validar-dados
set VALIDAR_DADOS_EXIT=%ERRORLEVEL%

echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo 📋 ETAPA 3/4: AUDITORIA DE DADOS (ÚLTIMAS 24 HORAS)
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

java -jar "target\extrator.jar" --auditoria
set AUDITORIA_EXIT=%ERRORLEVEL%

echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo 📋 ETAPA 4/4: VALIDAÇÃO ESPECÍFICA DE MANIFESTOS
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

java -jar "target\extrator.jar" --validar-manifestos
set VALIDAR_MANIFESTOS_EXIT=%ERRORLEVEL%

echo.
echo ╔══════════════════════════════════════════════════════════════════════════════╗
echo ║                                                                              ║
echo ║                    📊 RESUMO DO RELATÓRIO COMPLETO                           ║
echo ║                                                                              ║
echo ╚══════════════════════════════════════════════════════════════════════════════╝
echo.

set TOTAL_ERROS=0

if %VALIDAR_EXIT% equ 0 (
    echo    ✅ Validação de Acesso: SUCESSO
) else (
    echo    ❌ Validação de Acesso: FALHOU (Exit Code: %VALIDAR_EXIT%^)
    set /a TOTAL_ERROS+=1
)

if %VALIDAR_DADOS_EXIT% equ 0 (
    echo    ✅ Validação Completa de Dados: SUCESSO
) else (
    echo    ❌ Validação Completa de Dados: FALHOU (Exit Code: %VALIDAR_DADOS_EXIT%^)
    set /a TOTAL_ERROS+=1
)

if %AUDITORIA_EXIT% equ 0 (
    echo    ✅ Auditoria de Dados: SUCESSO
) else (
    echo    ❌ Auditoria de Dados: FALHOU (Exit Code: %AUDITORIA_EXIT%^)
    set /a TOTAL_ERROS+=1
)

if %VALIDAR_MANIFESTOS_EXIT% equ 0 (
    echo    ✅ Validação de Manifestos: SUCESSO
) else (
    echo    ❌ Validação de Manifestos: FALHOU (Exit Code: %VALIDAR_MANIFESTOS_EXIT%^)
    set /a TOTAL_ERROS+=1
)

echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

if %TOTAL_ERROS% equ 0 (
    echo    ✅ TODAS AS VALIDAÇÕES FORAM CONCLUÍDAS COM SUCESSO!
    echo.
    echo    O sistema está íntegro e funcionando corretamente.
    echo.
) else (
    echo    ⚠️  ATENÇÃO: %TOTAL_ERROS% validação^ões falharam.
    echo.
    echo    Revise os resultados acima para identificar os problemas.
    echo    Verifique os logs na pasta 'logs' para mais detalhes.
    echo.
)

echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo 💡 Relatório completo gerado por @valentelucass
echo    lucasmac.dev@gmail.com
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

pause
endlocal
