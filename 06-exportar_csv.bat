@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

REM ================================================================
REM Script: 06-exportar_csv.bat
REM Finalidade: Exporta dados para CSV/Excel com menu interativo
REM Uso: Execute este script apos a extracao
REM ================================================================

echo ================================================================
echo   EXPORTADOR CSV - TODOS OS DADOS
echo ================================================================
echo.

echo Compilando projeto...
call "%~dp0mvn.bat" -q -DskipTests package
if errorlevel 1 (
    echo ERRO: Compilacao falhou
    echo.
    pause
    exit /b 1
)

REM Verificar se o JAR existe
if not exist "target\extrator.jar" (
    echo ERRO: JAR nao encontrado!
    echo.
    echo Execute primeiro: mvn clean package
    echo   ou: 05-compilar_projeto.bat
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   MENU DE EXPORTACAO
echo ================================================================
echo.
echo Escolha uma opcao:
echo.
echo   1. Exportar TODAS as tabelas
echo   2. Exportar uma tabela ESPECIFICA
echo   0. Cancelar
echo.
set /p OPCAO="Digite o numero da opcao: "

if "%OPCAO%"=="0" (
    echo Operacao cancelada.
    exit /b 0
)

if "%OPCAO%"=="1" (
    echo.
    echo Exportando TODAS as tabelas...
    echo.
    java -jar "target\extrator.jar" --exportar-csv
    goto :END
)

if "%OPCAO%"=="2" (
    echo.
    echo ================================================================
    echo   TABELAS DISPONIVEIS
    echo ================================================================
    echo.
    echo   1. cotacoes              (Cotacoes)
    echo   2. coletas               (Coletas)
    echo   3. contas_a_pagar        (Contas a Pagar)
    echo   4. faturas_por_cliente   (Faturas por Cliente)
    echo   5. faturas_graphql       (Faturas GraphQL)
    echo   6. fretes                (Fretes)
    echo   7. manifestos            (Manifestos)
    echo   8. localizacao_cargas    (Localizacao de Cargas)
    echo   9. page_audit            (Auditoria de Paginas)
    echo   0. Voltar ao menu anterior
    echo.
    set /p TABELA_NUM="Digite o numero da tabela: "
    
    if "!TABELA_NUM!"=="0" (
        echo Voltando ao menu anterior...
        goto :END
    )
    
    REM Mapear numero para nome da tabela
    set "TABELA="
    if "!TABELA_NUM!"=="1" set "TABELA=cotacoes"
    if "!TABELA_NUM!"=="2" set "TABELA=coletas"
    if "!TABELA_NUM!"=="3" set "TABELA=contas_a_pagar"
    if "!TABELA_NUM!"=="4" set "TABELA=faturas_por_cliente"
    if "!TABELA_NUM!"=="5" set "TABELA=faturas_graphql"
    if "!TABELA_NUM!"=="6" set "TABELA=fretes"
    if "!TABELA_NUM!"=="7" set "TABELA=manifestos"
    if "!TABELA_NUM!"=="8" set "TABELA=localizacao_cargas"
    if "!TABELA_NUM!"=="9" set "TABELA=page_audit"
    
    if not defined TABELA (
        echo ERRO: Numero invalido!
        pause
        exit /b 1
    )
    
    echo.
    echo Exportando tabela: !TABELA!
    echo.
    java -jar "target\extrator.jar" --exportar-csv "!TABELA!"
    goto :END
)

REM Opcao invalida
echo ERRO: Opcao invalida!
pause
exit /b 1

:END
if errorlevel 1 (
    echo.
    echo ================================================================
    echo   ERRO AO EXECUTAR EXPORTADOR
    echo ================================================================
    echo.
    echo Possiveis causas:
    echo 1. Classe ExportadorCSV nao existe
    echo 2. Banco de dados nao acessivel
    echo 3. Tabelas vazias
    echo.
    echo Solucao:
    echo 1. Execute a extracao primeiro: 01-executar_extracao_completa.bat
    echo 2. Verifique as configuracoes: 03-validar_configuracoes.bat
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   EXPORTACAO CONCLUIDA COM SUCESSO!
echo ================================================================
echo.

REM Verificar se a pasta exports existe e tem arquivos
if exist "exports\*.csv" (
    echo Arquivos CSV gerados:
    dir /B exports\*.csv
    echo.
    echo Abrindo pasta exports...
    start "" "exports"
) else (
    echo AVISO: Nenhum arquivo CSV encontrado em exports/
    echo Verifique se ha dados nas tabelas
)

echo.
echo ================================================================
echo   ARQUIVOS DISPONIVEIS
echo ================================================================
echo.
echo Os seguintes arquivos podem ter sido gerados:
echo.
echo   - contas_a_pagar.csv                 (Contas a Pagar)
echo   - faturas_por_cliente.csv            (Faturas por Cliente)
echo   - faturas_graphql.csv                (Faturas GraphQL/Ponte)
echo   - fretes.csv                          (Fretes)
echo   - coletas.csv                         (Coletas)
echo   - manifestos.csv                      (Manifestos)
echo   - cotacoes.csv                        (Cotacoes)
echo   - localizacao_cargas.csv              (Localizacao da Carga)
echo   - page_audit.csv                      (Auditoria de Paginas)
echo.
echo Verifique a pasta 'exports' para os arquivos gerados.
echo.
pause
endlocal
