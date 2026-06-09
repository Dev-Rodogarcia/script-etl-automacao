@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ================================================================
REM Script : database/executar_database.bat
REM Papel  : Executa scripts SQL Server do sistema ESL Cloud.
REM Regra  : toda mudanca estrutural nova em migrations deve ser refletida
REM          tambem nos scripts-base de database\tabelas, views, indices,
REM          validacao e demais artefatos afetados para permitir recriacao
REM          limpa e coerente do banco do zero.
REM
REM MODOS DE USO:
REM
REM   executar_database.bat
REM     Modo PADRAO (seguro):
REM     - Banco ja deve existir
REM     - Garante tabelas base sem usar DROP/CREATE DATABASE
REM     - Executa: migrations, indices, views, procedures, cargas iniciais, validacoes
REM     - NAO executa DROP/CREATE DATABASE
REM     - Idempotente - pode rodar multiplas vezes
REM
REM   executar_database.bat --recriar
REM     Modo DEV (destrutivo - requer confirmacao):
REM     - Apaga e recria o banco do zero
REM     - Executa: tabelas, migrations, indices, views, procedures, cargas iniciais, validacoes
REM     - ATENCAO: todos os dados serao perdidos
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
REM CARGAS INICIAIS:
REM   As fatos materializadas SQL sao carregadas obrigatoriamente apos publicar
REM   tabelas, migrations, indices, views e procedures. As dimensoes de usuarios
REM   sao sincronizadas automaticamente no inicio do ciclo Java de extracao.
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
if /i "%~1"=="--help" goto :MOSTRAR_AJUDA
if /i "%~1"=="-h" goto :MOSTRAR_AJUDA
if /i "%~1"=="/?" goto :MOSTRAR_AJUDA
if /i "%~1"=="--recriar" set "MODO_RECRIAR=1"

echo.
if "%MODO_RECRIAR%"=="1" (
    echo ============================================
    echo   EXECUTAR DATABASE - MODO DEV ^(--recriar^)
    echo   ATENCAO: banco sera apagado e recriado.
    echo ============================================
) else (
    echo ============================================
    echo   EXECUTAR DATABASE - MODO PADRAO
    echo   Banco existente - sem DROP/CREATE
    echo ============================================
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
call config.bat

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
echo.

REM ================================================================
REM MODO DEV: recriar banco do zero (apenas com --recriar)
REM ================================================================
if /i "%MODO_RECRIAR%"=="1" (
    call :RECRIAR_BANCO
    if errorlevel 2 exit /b 0
    if errorlevel 1 exit /b 1
)

REM ================================================================
REM AMBOS OS MODOS: tabelas base, migrations, indices, views, validacoes
REM ================================================================

REM --- Tabelas base (idempotente - cria faltantes sem recriar o banco) ---
if /i not "%MODO_RECRIAR%"=="1" (
    call :GARANTIR_TABELAS_BASE
    if errorlevel 1 exit /b 1
)

REM --- Migrations (criticas - para em erro) ---
echo [ETAPA] Migrations...
for %%F in (
    "migrations\001_criar_tabela_schema_migrations.sql"
    "migrations\002_corrigir_constraint_manifestos.sql"
    "migrations\004_adicionar_request_hour_coletas.sql"
    "migrations\005_alinhar_sys_execution_history_schema.sql"
    "migrations\006_alterar_fretes_indicadores_gestao.sql"
    "migrations\007_adicionar_fk_seletiva_manifestos_coletas.sql"
    "migrations\008_criar_tabela_sys_replay_idempotency.sql"
    "migrations\009_criar_tabela_sys_reconciliation_quarantine.sql"
    "migrations\010_harden_coletas_sequence_code.sql"
    "migrations\011_alinhar_chave_merge_manifestos_orfaos.sql"
    "migrations\012_adicionar_frete_cortesia.sql"
    "migrations\013_ajustar_precisao_cubagem_fretes.sql"
    "migrations\014_criar_tabelas_raster.sql"
    "migrations\015_adicionar_cliente_cnpj_faturas_por_cliente.sql"
    "migrations\016_materializar_faturamento_fretes.sql"
    "migrations\017_localizacao_cargas_dashboard_operacional.sql"
    "migrations\018_adicionar_indice_coletas_request_date_dashboard.sql"
    "migrations\019_adicionar_comprovante_fretes_performance.sql"
    "migrations\020_adicionar_tipo_motorista_manifestos.sql"
    "migrations\021_materializar_comprovante_inventario.sql"
    "migrations\022_corrigir_volumes_fretes_faturamento.sql"
    "migrations\023_adicionar_noop_count_log_extracoes.sql"
    "migrations\024_drop_faturas_graphql.sql"
    "migrations\025_materializar_chave_responsavel_destino.sql"
    "migrations\026_materializar_chave_usuario_cotacoes.sql"
    "migrations\027_adicionar_excluido_na_origem.sql"
    "migrations\028_corrigir_chave_unica_manifestos.sql"
    "migrations\029_criar_fato_gestao_vista_fretes.sql"
    "migrations\030_criar_fato_gestao_vista_coletores.sql"
    "migrations\031_criar_fato_fretes_faturamento.sql"
    "migrations\032_criar_fato_gestao_vista_faturas.sql"
    "migrations\033_tuning_indices_fatos.sql"
    "migrations\034_adicionar_hash_linha_usuarios.sql"
    "migrations\035_drop_views_legadas_powerbi.sql"
    "migrations\037_adicionar_status_fatura.sql"
    "migrations\038_atualizar_min_frete_cotacoes_matriz_uf.sql"
    "migrations\039_criar_dim_calendario_referencia_faturamento.sql"
) do (
    if not exist %%F (
        echo   [SKIP] Nao encontrada: %%~F
    ) else (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F" -b
        if errorlevel 1 (
            echo [ERRO] Falha critica na migration: %%~F
            set "SQLCMDPASSWORD="
            if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
            exit /b 1
        )
    )
)
echo [OK] Migrations concluidas.
echo.

REM --- Indices (nao-criticos - avisa e continua) ---
echo [ETAPA] Indices de performance...
for %%F in (
    "indices\001_criar_indices_performance.sql"
) do (
    if exist %%F (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F"
        if errorlevel 1 echo   [AVISO] Indice pode ja existir: %%~F
    )
)
echo [OK] Indices concluidos.
echo.

REM --- Views Power BI, analiticas e Dimensao (nao-criticas - avisa e continua) ---
echo [ETAPA] Views ^(Power BI + Analiticas + Dimensao^)...
for %%F in (
    "views\011_criar_view_faturas_por_cliente_powerbi.sql"
    "views\012_criar_view_fretes_powerbi.sql"
    "views\013_criar_view_coletas_powerbi.sql"
    "views\015_criar_view_cotacoes_powerbi.sql"
    "views\016_criar_view_contas_a_pagar_powerbi.sql"
    "views\017_criar_view_localizacao_cargas_powerbi.sql"
    "views\018_criar_view_manifestos_powerbi.sql"
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
    if exist %%F (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F"
        if errorlevel 1 echo   [AVISO] View pode ja existir: %%~F
    )
)
echo [OK] Views concluidas.
echo.

REM --- Stored Procedures (criticas - para em erro) ---
echo [ETAPA] Stored Procedures...
for %%F in (
    "procedures\001_criar_sp_carga_fato_gestao_vista_fretes.sql"
    "procedures\002_criar_sp_carga_fato_gestao_vista_coletores.sql"
    "procedures\003_criar_sp_carga_fato_fretes_faturamento.sql"
    "procedures\004_criar_sp_carga_fato_gestao_vista_faturas.sql"
) do (
    if exist %%F (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F" -b
        if errorlevel 1 (
            echo [ERRO] Falha critica na procedure: %%~F
            set "SQLCMDPASSWORD="
            if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
            exit /b 1
        )
    )
)
echo [OK] Stored Procedures concluidas.
echo.

REM --- Cargas iniciais obrigatorias das fatos SQL (criticas - para em erro) ---
echo [ETAPA] Cargas iniciais das fatos BI...
echo   [EXEC] dbo.sp_carga_fato_gestao_vista_fretes
sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -Q "EXEC dbo.sp_carga_fato_gestao_vista_fretes;" -b
if errorlevel 1 (
    echo [ERRO] Falha na carga materializada Gestao a Vista ^(fretes^).
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo   [EXEC] dbo.sp_carga_fato_gestao_vista_coletores
sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -Q "EXEC dbo.sp_carga_fato_gestao_vista_coletores;" -b
if errorlevel 1 (
    echo [ERRO] Falha na carga materializada Gestao a Vista ^(coletores^).
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo   [EXEC] dbo.sp_carga_fato_fretes_faturamento
sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -Q "EXEC dbo.sp_carga_fato_fretes_faturamento;" -b
if errorlevel 1 (
    echo [ERRO] Falha na carga materializada de Faturamento de Fretes.
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo   [EXEC] dbo.sp_carga_fato_gestao_vista_faturas
sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -Q "EXEC dbo.sp_carga_fato_gestao_vista_faturas;" -b
if errorlevel 1 (
    echo [ERRO] Falha na carga materializada de Faturas por Cliente.
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo [OK] Cargas iniciais das fatos BI concluidas.
echo [INFO] Dimensoes de usuarios sao sincronizadas automaticamente pelo ciclo Java de extracao.
echo.

REM --- Validacoes de leitura (seguras, sem scripts destrutivos) ---
REM Excluidos: 027 diagnosticar_null, 030 api_vs_banco, 031 limpar_dados
echo [ETAPA] Validacoes...
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
) do (
    if exist %%F (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F" -b
        if errorlevel 1 echo   [AVISO] Validacao retornou aviso: %%~F
    )
)
echo [OK] Validacoes concluidas.
echo.

REM Limpar senha da memoria
set "SQLCMDPASSWORD="

echo ============================================
if "%MODO_RECRIAR%"=="1" (
    echo   CONCLUIDO - Banco recriado e configurado.
) else (
    echo   CONCLUIDO - Scripts SQL executados sem recriar o banco.
)
echo ============================================
echo.
if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
exit /b 0

:MOSTRAR_AJUDA
echo.
echo Uso:
echo   executar_database.bat
echo      Atualiza um banco existente. Cria tabelas faltantes, aplica migrations,
echo      indices, views, procedures, cargas iniciais SQL e validacoes seguras.
echo.
echo   executar_database.bat --recriar
echo      Apaga e recria o banco definido em config.bat ^(ex.: ETL_SISTEMA^).
echo      Depois cria tabelas base, aplica migrations, indices, views, procedures,
echo      cargas iniciais SQL e validacoes.
echo      Requer digitacao de RECRIAR para confirmar.
echo.
echo Configuracao:
echo   Copie config_exemplo.bat para config.bat e ajuste DB_SERVER, DB_PORT,
echo   DB_NAME, DB_USER, DB_PASSWORD e SQLCMD_EXTRA_ARGS.
echo.
echo Observacoes:
echo   - Pare o daemon antes de recriar ou apagar o banco.
echo   - Scripts destrutivos de validacao/limpeza nao rodam automaticamente.
echo   - Dimensoes de usuarios entram no primeiro passo do ciclo Java de extracao.
echo   - O banco SQLite de autenticacao nao e recriado por este script.
echo.
exit /b 0

:GARANTIR_TABELAS_BASE
echo [ETAPA] Garantindo tabelas base...
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
) do (
    if not exist %%F (
        echo [ERRO] Script nao encontrado: %%~F
        set "SQLCMDPASSWORD="
        if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
        exit /b 1
    )
    echo   [EXEC] %%~F
    sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "%%~F" -b
    if errorlevel 1 (
        echo [ERRO] Falha em: %%~F
        set "SQLCMDPASSWORD="
        if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
        exit /b 1
    )
)
echo [OK] Tabelas base garantidas.
echo.
exit /b 0

:RECRIAR_BANCO
echo ATENCAO: Esta operacao vai APAGAR todos os dados do banco [%DB_NAME%].
echo.
set "CONFIRMA="
set /p "CONFIRMA=Confirma a recreacao do banco? (RECRIAR/N): "
if /i not "%CONFIRMA%"=="RECRIAR" (
    echo Operacao cancelada.
    set "SQLCMDPASSWORD="
    exit /b 2
)

echo.
echo [EXEC] DROP / CREATE DATABASE [%DB_NAME%]...
sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% %AUTH_CMD% -d master -Q "IF DB_ID('%DB_NAME%') IS NOT NULL BEGIN ALTER DATABASE [%DB_NAME%] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [%DB_NAME%]; END; CREATE DATABASE [%DB_NAME%];"
if errorlevel 1 (
    echo [ERRO] Falha ao recriar banco de dados: %DB_NAME%
    set "SQLCMDPASSWORD="
    if /i not "%EXTRATOR_DB_SILENT%"=="1" pause
    exit /b 1
)
echo [OK] Banco [%DB_NAME%] recriado.
echo.

call :GARANTIR_TABELAS_BASE
if errorlevel 1 exit /b 1

REM Seguranca SQL Server (permissoes - apenas no recriar)
if exist "seguranca\024_configurar_permissoes_usuario.sql" (
    echo   [EXEC] seguranca\024_configurar_permissoes_usuario.sql
    sqlcmd %SQLCMD_FLAGS% -S %DB_SERVER_TARGET% -d %DB_NAME% %AUTH_CMD% -i "seguranca\024_configurar_permissoes_usuario.sql" -b
    if errorlevel 1 echo   [AVISO] Permissoes retornaram erro - verifique manualmente.
    echo.
)
exit /b 0
