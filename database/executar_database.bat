@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ================================================================
REM Script : database/executar_database.bat
REM Papel  : Publica o schema SQL Server do sistema ESL Cloud.
REM Regra  : toda mudanca estrutural nova em migrations deve ser refletida
REM          tambem nos scripts-base de database\tabelas, views, indices,
REM          validacao e demais artefatos afetados para permitir recriacao
REM          limpa e coerente do banco do zero.
REM
REM MODOS DE USO:
REM
REM   executar_database.bat
REM     Modo PADRAO (seguro e rapido):
REM     - Banco ja deve existir
REM     - Garante tabelas base sem usar DROP/CREATE DATABASE
REM     - Executa DDL em lote unico via sqlcmd master temporario
REM     - Executa: tabelas, migrations novas na raiz, indices, views,
REM       contrato critico e procedures
REM     - NAO executa cargas materializadas BI
REM     - NAO executa DROP/CREATE DATABASE
REM
REM   executar_database.bat --com-cargas
REM     Modo PADRAO com materializacao:
REM     - Executa todo o DDL do modo padrao
REM     - Executa as 5 procedures pesadas de materializacao BI
REM     - Executa validacoes de leitura apos as cargas
REM     - Use somente com o daemon Java parado ou em janela controlada
REM
REM   executar_database.bat --recriar
REM     Modo DEV (destrutivo - requer confirmacao):
REM     - Apaga e recria o banco do zero
REM     - Executa DDL em lote unico via sqlcmd master temporario
REM     - NAO executa cargas materializadas BI sem --com-cargas
REM     - ATENCAO: todos os dados serao perdidos
REM
REM   executar_database.bat --recriar --com-cargas
REM     Recria o banco e, ao final, executa as cargas BI e validacoes.
REM
REM   executar_database.bat --help
REM     Mostra um resumo operacional rapido sem conectar no SQL Server.
REM
REM AUTENTICACAO SQL SERVER:
REM   Windows Auth (padrao): deixe DB_USER vazio em config.bat
REM   SQL Auth              : preencha DB_USER e DB_PASSWORD em config.bat
REM   NAO use "sa". Crie um usuario dedicado com permissoes minimas.
REM
REM OPCOES ADICIONAIS (config.bat):
REM   DB_PORT           : porta do SQL Server (ex.: 1433)
REM   SQLCMD_EXTRA_ARGS : flags extras do sqlcmd (ex.: -C para confiar no certificado)
REM
REM CARGAS BI:
REM   As fatos materializadas SQL somente sao carregadas quando --com-cargas
REM   for informado. Sem essa flag, este script publica DDL e finaliza sem
REM   disputar locks pesados com o daemon Java.
REM
REM BANCO SQLite DE AUTENTICACAO:
REM   Gerenciado exclusivamente pela aplicacao Java (extrator.jar).
REM   Caminho: C:\ProgramData\ExtratorESL\security\users.db
REM   Scripts em database/security_sqlite/ sao apenas referencia.
REM   NAO sao executados via sqlcmd neste script.
REM ================================================================

chcp 65001 >nul
cd /d "%~dp0"

REM --- Detectar modo ---
set "MODO_RECRIAR=0"
set "MODO_FORCE=0"
set "MODO_COM_CARGAS=0"
set "MASTER_SQL=%TEMP%\run_master.sql"

:PARSE_ARGS
if "%~1"=="" goto :ARGS_OK
if /i "%~1"=="--help" goto :MOSTRAR_AJUDA
if /i "%~1"=="-h" goto :MOSTRAR_AJUDA
if /i "%~1"=="/?" goto :MOSTRAR_AJUDA
if /i "%~1"=="--recriar" set "MODO_RECRIAR=1"
if /i "%~1"=="--force" set "MODO_FORCE=1"
if /i "%~1"=="--com-cargas" set "MODO_COM_CARGAS=1"
shift
goto :PARSE_ARGS

:ARGS_OK

echo.
if "%MODO_RECRIAR%"=="1" (
    echo ============================================
    echo   EXECUTAR DATABASE - MODO DEV ^(--recriar^)
    echo   ATENCAO: banco sera apagado e recriado.
    if "%MODO_FORCE%"=="1" echo   FORCE: confirmacao manual desabilitada.
    echo ============================================
) else (
    echo ============================================
    echo   EXECUTAR DATABASE - MODO PADRAO
    echo   Banco existente - sem DROP/CREATE
    echo ============================================
)
if "%MODO_COM_CARGAS%"=="1" (
    echo   Cargas BI: ATIVADAS ^(--com-cargas^)
) else (
    echo   Cargas BI: perguntar ao final ^(CI/CD assume N sem --com-cargas^)
)
echo.

REM --- 1. Verificar config.bat ---
if not exist "config.bat" (
    echo [ERRO] Arquivo config.bat nao encontrado!
    echo.
    echo Copie config_exemplo.bat para config.bat e preencha:
    echo   DB_SERVER, DB_NAME
    echo   DB_USER e DB_PASSWORD apenas para autenticacao SQL
    echo   ^(deixe vazios para usar Windows Authentication^)
    echo.
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)

REM --- 2. Carregar config.bat ---
call "%~dp0config.bat"

REM --- 3. Validar variaveis obrigatorias ---
if "%DB_SERVER%"=="" (
    echo [ERRO] DB_SERVER nao definido no config.bat
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
if "%DB_NAME%"=="" (
    echo [ERRO] DB_NAME nao definido no config.bat
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)

set "DB_SERVER_TARGET=%DB_SERVER%"
if not "%DB_PORT%"=="" set "DB_SERVER_TARGET=%DB_SERVER%,%DB_PORT%"
REM Leitura explicita em UTF-8 para preservar aliases/acento dos scripts .sql
set "SQLCMD_FLAGS=-I -f 65001"
if not "%SQLCMD_EXTRA_ARGS%"=="" set "SQLCMD_FLAGS=%SQLCMD_FLAGS% %SQLCMD_EXTRA_ARGS%"

REM --- 4. Verificar sqlcmd ---
where sqlcmd >nul 2>nul
if errorlevel 1 (
    echo [ERRO] sqlcmd nao encontrado no PATH.
    echo.
    echo Instale as "SQL Server Command Line Utilities" ou adicione o diretorio
    echo do SQL Server ao PATH.
    echo Ex: C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn
    echo.
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)

REM --- 5. Definir autenticacao: Windows (-E) ou SQL (-U + SQLCMDPASSWORD) ---
if "%DB_USER%"=="" (
    set "AUTH_CMD=-E"
    set "SQLCMDPASSWORD="
    echo Autenticacao: Windows ^(integrada^)
) else (
    if "%DB_PASSWORD%"=="" (
        echo [ERRO] DB_USER definido mas DB_PASSWORD esta vazio no config.bat
        if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
        exit /b 1
    )
    set "AUTH_CMD=-U %DB_USER%"
    set "SQLCMDPASSWORD=%DB_PASSWORD%"
    echo Autenticacao: SQL ^(%DB_USER%^)
)
echo Servidor: %DB_SERVER_TARGET%  ^|  Banco: %DB_NAME%
echo Opcoes sqlcmd: %SQLCMD_FLAGS%
echo Master SQL: %MASTER_SQL%
echo.

if /i "%MODO_RECRIAR%"=="1" (
    call :CONFIRMAR_RECRIACAO
    if errorlevel 2 exit /b 0
    if errorlevel 1 exit /b 1
)

echo [ETAPA] Montando master SQL temporario...
call :MONTAR_MASTER_SQL
if errorlevel 1 (
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo [OK] Master SQL montado.
echo.

echo [ETAPA] Executando master SQL em uma unica sessao sqlcmd...
if /i "%MODO_RECRIAR%"=="1" (
    sqlcmd %SQLCMD_FLAGS% -S "%DB_SERVER_TARGET%" %AUTH_CMD% -d master -v DB_NAME="%DB_NAME%" -i "%MASTER_SQL%" -b
) else (
    sqlcmd %SQLCMD_FLAGS% -S "%DB_SERVER_TARGET%" -d "%DB_NAME%" %AUTH_CMD% -i "%MASTER_SQL%" -b
)
if errorlevel 1 (
    echo [ERRO] Falha na execucao do master SQL.
    echo [INFO] Master preservado para diagnostico: %MASTER_SQL%
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo [OK] Master SQL executado.
echo.

del /q "%MASTER_SQL%" >nul 2>nul

if /i "%MODO_COM_CARGAS%"=="0" (
    call :PROMPT_CARGAS_POS_DDL
    if errorlevel 1 (
        set "SQLCMDPASSWORD="
        if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
        exit /b 1
    )
)

set "SQLCMDPASSWORD="

echo ============================================
if "%MODO_RECRIAR%"=="1" (
    if "%MODO_COM_CARGAS%"=="1" (
        echo   CONCLUIDO - Banco recriado, configurado e materializado.
    ) else (
        echo   CONCLUIDO - Banco recriado e schema publicado sem cargas BI.
    )
) else (
    if "%MODO_COM_CARGAS%"=="1" (
        echo   CONCLUIDO - Schema publicado e cargas BI executadas.
    ) else (
        echo   CONCLUIDO - Schema publicado sem executar cargas BI.
    )
)
echo ============================================
echo.
if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
exit /b 0

:MOSTRAR_AJUDA
echo.
echo Uso:
echo   executar_database.bat
echo      Atualiza um banco existente. Publica tabelas, migrations novas da raiz,
echo      indices, views, procedures e contrato critico em uma unica sessao sqlcmd.
echo      Ao final pergunta se deve executar cargas BI. Em modo silencioso assume N.
echo.
echo   executar_database.bat --com-cargas
echo      Executa o fluxo acima e, ao final, materializa as 5 fatos BI e roda
echo      validacoes de leitura. Use com o daemon parado ou em janela controlada.
echo.
echo   executar_database.bat --recriar
echo      Apaga e recria o banco definido em config.bat ^(ex.: ETL_SISTEMA^).
echo      Depois publica o schema em lote unico. Sem --com-cargas, pergunta
echo      interativamente se deve executar cargas BI. Requer digitacao de RECRIAR.
echo      Use --force junto com --recriar para automacoes controladas.
echo.
echo Configuracao:
echo   Copie config_exemplo.bat para config.bat e ajuste DB_SERVER, DB_PORT,
echo   DB_NAME, DB_USER, DB_PASSWORD e SQLCMD_EXTRA_ARGS.
echo.
echo Observacoes:
echo   - Pare o daemon antes de recriar, apagar ou materializar o banco.
echo   - Migrations antigas consolidadas ficam em migrations\historico_arquivado.
echo   - Apenas scripts .sql diretamente em migrations\ sao executados.
echo   - Scripts destrutivos de validacao/limpeza nao rodam automaticamente.
echo   - Dimensoes de usuarios entram no primeiro passo do ciclo Java de extracao.
echo   - O banco SQLite de autenticacao nao e recriado por este script.
echo.
exit /b 0

:CONFIRMAR_RECRIACAO
echo ATENCAO: Esta operacao vai APAGAR todos os dados do banco [%DB_NAME%].
echo.
set "CONFIRMA="
if /i "%MODO_FORCE%"=="1" (
    echo [FORCE] Confirmacao manual ignorada por --force.
    exit /b 0
)
set /p "CONFIRMA=Confirma a recreacao do banco? (RECRIAR/N): "
if /i not "%CONFIRMA%"=="RECRIAR" (
    echo Operacao cancelada.
    set "SQLCMDPASSWORD="
    exit /b 2
)
exit /b 0

:MONTAR_MASTER_SQL
if exist "%MASTER_SQL%" del /q "%MASTER_SQL%" >nul 2>nul
> "%MASTER_SQL%" echo :ON ERROR EXIT
>> "%MASTER_SQL%" echo SET NOCOUNT ON;
>> "%MASTER_SQL%" echo GO

if /i "%MODO_RECRIAR%"=="1" (
    call :MASTER_ADD_RECRIAR
    if errorlevel 1 exit /b 1
)

call :MASTER_ADD_SECTION "Tabelas base"
for %%F in (
    "tabelas\001_criar_tabela_coletas.sql"
    "tabelas\002_criar_tabela_fretes.sql"
    "tabelas\003_criar_tabela_manifestos.sql"
    "tabelas\004_criar_tabela_cotacoes.sql"
    "tabelas\005_criar_tabela_localizacao_cargas.sql"
    "tabelas\006_criar_tabela_contas_a_pagar.sql"
    "tabelas\007_criar_tabela_faturas_por_cliente.sql"
    "tabelas\008_criar_tabela_dim_calendario.sql"
    "tabelas\009_criar_tabela_log_extracoes.sql"
    "tabelas\010_criar_tabela_page_audit.sql"
    "tabelas\011_criar_tabela_dim_usuarios.sql"
    "tabelas\012_criar_tabela_sys_execution_history.sql"
    "tabelas\013_criar_tabela_sys_auditoria_temp.sql"
    "tabelas\014_criar_tabela_sys_execution_audit.sql"
    "tabelas\015_criar_tabela_sys_execution_watermark.sql"
    "tabelas\016_alter_tabela_dim_usuarios_estado.sql"
    "tabelas\017_criar_tabela_dim_usuarios_historico.sql"
    "tabelas\018_criar_tabela_schema_migrations.sql"
    "tabelas\019_criar_tabela_etl_invalid_records.sql"
    "tabelas\020_criar_tabela_inventario.sql"
    "tabelas\021_criar_tabela_sinistros.sql"
    "tabelas\022_criar_tabela_sys_replay_idempotency.sql"
    "tabelas\023_criar_tabela_sys_reconciliation_quarantine.sql"
    "tabelas\024_criar_tabela_raster_viagens.sql"
    "tabelas\025_criar_tabela_raster_viagem_paradas.sql"
    "tabelas\026_criar_tabela_localizacao_cargas_regiao_destino_alias.sql"
    "tabelas\027_criar_tabela_manifestos_frota_propria_cnpjs.sql"
    "tabelas\028_criar_tabela_fato_gestao_vista_fretes.sql"
    "tabelas\029_criar_tabela_fato_gestao_vista_coletores.sql"
    "tabelas\030_criar_tabela_fato_fretes_faturamento.sql"
    "tabelas\031_criar_tabela_fato_gestao_vista_faturas.sql"
    "tabelas\032_criar_tabela_fato_gestao_vista_manifestos.sql"
    "tabelas\033_criar_tabela_regras_atribuicao_filial.sql"
) do (
    call :MASTER_ADD_REQUIRED "%%~F"
    if errorlevel 1 exit /b 1
)

if /i "%MODO_RECRIAR%"=="1" (
    if exist "seguranca\024_configurar_permissoes_usuario.sql" (
        call :MASTER_ADD_SECTION "Seguranca SQL Server"
        call :MASTER_ADD_REQUIRED "seguranca\024_configurar_permissoes_usuario.sql"
        if errorlevel 1 exit /b 1
    )
)

call :MASTER_ADD_SECTION "Migrations novas da raiz"
set "MIGRATIONS_COUNT=0"
for /f "delims=" %%F in ('dir /b /a-d /on "migrations\*.sql" 2^>nul') do (
    call :MASTER_ADD_REQUIRED "migrations\%%F"
    if errorlevel 1 exit /b 1
    set /a MIGRATIONS_COUNT+=1 >nul
)
if "%MIGRATIONS_COUNT%"=="0" (
    echo   [INFO] Nenhuma migration nova encontrada em migrations\*.sql
    call :MASTER_ADD_INFO "Nenhuma migration nova encontrada em migrations\\*.sql"
)

call :MASTER_ADD_SECTION "Indices de performance"
for %%F in (
    "indices\001_criar_indices_performance.sql"
    "indices\002_criar_indices_fato_gestao_vista_manifestos.sql"
) do (
    call :MASTER_ADD_OPTIONAL "%%~F"
    if errorlevel 1 exit /b 1
)

call :MASTER_ADD_SECTION "Views Power BI, Analiticas e Dimensao"
for %%F in (
    "views\011_criar_view_faturas_por_cliente_powerbi.sql"
    "views\012_criar_view_fretes_powerbi.sql"
    "views\013_criar_view_coletas_powerbi.sql"
    "views\015_criar_view_cotacoes_powerbi.sql"
    "views\016_criar_view_contas_a_pagar_powerbi.sql"
    "views\017_criar_view_localizacao_cargas_powerbi.sql"
    "views\018_criar_view_manifestos_powerbi.sql"
    "views\025_criar_view_fato_manifestos_dash.sql"
    "views\020_criar_view_inventario_powerbi.sql"
    "views\021_criar_view_sinistros_powerbi.sql"
    "views\022_criar_view_raster_sm_transit_time.sql"
    "views\019_criar_view_bi_monitoramento.sql"
    "views-dimensao\019_criar_view_dim_filiais.sql"
    "views-dimensao\020_criar_view_dim_clientes.sql"
    "views-dimensao\021_criar_view_dim_veiculos.sql"
    "views-dimensao\022_criar_view_dim_motoristas.sql"
    "views-dimensao\023_criar_view_dim_planocontas.sql"
    "views-dimensao\024_criar_view_dim_usuarios.sql"
) do (
    call :MASTER_ADD_OPTIONAL "%%~F"
    if errorlevel 1 exit /b 1
)

call :MASTER_ADD_SECTION "Contrato critico da view de fretes"
call :MASTER_ADD_REQUIRED "validacao\042_validar_contrato_dashboard_performance.sql"
if errorlevel 1 exit /b 1

call :MASTER_ADD_SECTION "Stored Procedures"
for %%F in (
    "procedures\001_criar_sp_carga_fato_gestao_vista_fretes.sql"
    "procedures\002_criar_sp_carga_fato_gestao_vista_coletores.sql"
    "procedures\003_criar_sp_carga_fato_fretes_faturamento.sql"
    "procedures\004_criar_sp_carga_fato_gestao_vista_faturas.sql"
    "procedures\005_criar_sp_carga_fato_gestao_vista_manifestos.sql"
) do (
    call :MASTER_ADD_REQUIRED "%%~F"
    if errorlevel 1 exit /b 1
)

if /i "%MODO_COM_CARGAS%"=="1" (
    echo [ETAPA] Cargas materializadas BI
    >> "%MASTER_SQL%" echo PRINT N'[ETAPA] Cargas materializadas BI';
    >> "%MASTER_SQL%" echo PRINT N'[INFO] LOCK_TIMEOUT das cargas BI: 10000 ms';
    >> "%MASTER_SQL%" echo SET LOCK_TIMEOUT 10000;
    >> "%MASTER_SQL%" echo GO
    call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_fretes"
    call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_coletores"
    call :MASTER_ADD_EXEC "dbo.sp_carga_fato_fretes_faturamento"
    call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_faturas"
    call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_manifestos"

    echo [ETAPA] Validacoes de leitura
    >> "%MASTER_SQL%" echo PRINT N'[ETAPA] Validacoes de leitura';
    >> "%MASTER_SQL%" echo GO
    for %%F in (
        "validacao\025_validar_views_dimensao.sql"
        "validacao\026_validar_tipo_destroy_user_id.sql"
        "validacao\028_validacao_rapida_extracao.sql"
        "validacao\032_validar_orfaos_manifestos_coletas.sql"
        "validacao\034_validar_schema_recriacao.sql"
        "validacao\036_validar_volumes_fretes_faturamento.sql"
        "validacao\038_validar_fato_gestao_vista_fretes.sql"
        "validacao\039_validar_fato_gestao_vista_coletores.sql"
        "validacao\040_validar_fato_fretes_faturamento.sql"
        "validacao\041_validar_fato_gestao_vista_faturas.sql"
        "validacao\043_validar_indice_performance_fretes.sql"
        "validacao\045_validar_fato_gestao_vista_manifestos.sql"
    ) do (
        call :MASTER_ADD_OPTIONAL "%%~F"
        if errorlevel 1 exit /b 1
    )
) else (
    call :MASTER_ADD_INFO "Cargas materializadas BI nao incluidas no master DDL. Confirme no prompt pos-DDL ou use --com-cargas."
)

exit /b 0

:PROMPT_CARGAS_POS_DDL
set "RESPOSTA_CARGAS=N"
if /i "%EXTRATOR_DB_SILENT%"=="1" (
    echo [INFO] Modo silencioso sem --com-cargas: cargas BI ignoradas ^(assumido N^).
    exit /b 0
)
echo.
set /p "RESPOSTA_CARGAS=Deseja reprocessar as tabelas Fato agora (Recomendado apos alterar regras de negocio)? (S/N): "
if /i "%RESPOSTA_CARGAS%"=="S" (
    set "MODO_COM_CARGAS=1"
    call :EXECUTAR_CARGAS_POS_DDL
    if errorlevel 1 exit /b 1
    exit /b 0
)
echo [INFO] Cargas BI ignoradas por escolha do usuario.
exit /b 0

:EXECUTAR_CARGAS_POS_DDL
echo.
echo [ETAPA] Montando master SQL temporario para cargas BI...
if exist "%MASTER_SQL%" del /q "%MASTER_SQL%" >nul 2>nul
> "%MASTER_SQL%" echo :ON ERROR EXIT
>> "%MASTER_SQL%" echo SET NOCOUNT ON;
>> "%MASTER_SQL%" echo SET LOCK_TIMEOUT 10000;
>> "%MASTER_SQL%" echo GO
>> "%MASTER_SQL%" echo PRINT N'[ETAPA] Cargas materializadas BI';
>> "%MASTER_SQL%" echo PRINT N'[INFO] LOCK_TIMEOUT das cargas BI: 10000 ms';
>> "%MASTER_SQL%" echo GO
call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_fretes"
call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_coletores"
call :MASTER_ADD_EXEC "dbo.sp_carga_fato_fretes_faturamento"
call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_faturas"
call :MASTER_ADD_EXEC "dbo.sp_carga_fato_gestao_vista_manifestos"

echo [ETAPA] Executando cargas BI em uma unica sessao sqlcmd...
sqlcmd %SQLCMD_FLAGS% -S "%DB_SERVER_TARGET%" -d "%DB_NAME%" %AUTH_CMD% -i "%MASTER_SQL%" -b
if errorlevel 1 (
    echo [ERRO] Falha na execucao das cargas BI.
    echo [INFO] Master preservado para diagnostico: %MASTER_SQL%
    exit /b 1
)
del /q "%MASTER_SQL%" >nul 2>nul
echo [OK] Cargas BI executadas.
echo.
exit /b 0

:MASTER_ADD_RECRIAR
>> "%MASTER_SQL%" echo PRINT N'[CHECK] Permissao para recriar banco $(DB_NAME)';
>> "%MASTER_SQL%" echo IF ISNULL(IS_SRVROLEMEMBER('sysadmin'), 0^) ^<^> 1 AND ISNULL(IS_SRVROLEMEMBER('dbcreator'), 0^) ^<^> 1 AND ISNULL(HAS_PERMS_BY_NAME(NULL, NULL, 'CREATE ANY DATABASE'), 0^) ^<^> 1
>> "%MASTER_SQL%" echo BEGIN
>> "%MASTER_SQL%" echo     RAISERROR('Login atual nao tem permissao para CREATE DATABASE em master.', 16, 1^);
>> "%MASTER_SQL%" echo END
>> "%MASTER_SQL%" echo GO
>> "%MASTER_SQL%" echo PRINT N'[EXEC] DROP / CREATE DATABASE [$(DB_NAME)]';
>> "%MASTER_SQL%" echo IF DB_ID(N'$(DB_NAME)'^) IS NOT NULL
>> "%MASTER_SQL%" echo BEGIN
>> "%MASTER_SQL%" echo     ALTER DATABASE [$(DB_NAME)] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
>> "%MASTER_SQL%" echo     DROP DATABASE [$(DB_NAME)];
>> "%MASTER_SQL%" echo END
>> "%MASTER_SQL%" echo CREATE DATABASE [$(DB_NAME)];
>> "%MASTER_SQL%" echo GO
>> "%MASTER_SQL%" echo USE [$(DB_NAME)];
>> "%MASTER_SQL%" echo GO
exit /b 0

:MASTER_ADD_SECTION
echo [ETAPA] %~1
>> "%MASTER_SQL%" echo PRINT N'[ETAPA] %~1';
>> "%MASTER_SQL%" echo GO
exit /b 0

:MASTER_ADD_INFO
>> "%MASTER_SQL%" echo PRINT N'[INFO] %~1';
>> "%MASTER_SQL%" echo GO
exit /b 0

:MASTER_ADD_OPTIONAL
if not exist "%~1" (
    echo   [SKIP] %~1
    exit /b 0
)
call :MASTER_ADD_REQUIRED "%~1"
if errorlevel 1 exit /b 1
exit /b 0

:MASTER_ADD_REQUIRED
if not exist "%~1" (
    echo [ERRO] Script nao encontrado: %~1
    exit /b 1
)
echo   [ADD] %~1
>> "%MASTER_SQL%" echo PRINT N'[EXEC] %~1';
>> "%MASTER_SQL%" echo GO
>> "%MASTER_SQL%" echo :r "%~dp0%~1"
>> "%MASTER_SQL%" echo GO
exit /b 0

:MASTER_ADD_EXEC
echo   [ADD] EXEC %~1
>> "%MASTER_SQL%" echo PRINT N'[EXEC] %~1';
>> "%MASTER_SQL%" echo EXEC %~1;
>> "%MASTER_SQL%" echo GO
exit /b 0
