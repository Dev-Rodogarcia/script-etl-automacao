@echo off
REM ============================================
REM Script para executar todas as databases SQL
REM Execute este script UMA VEZ antes de colocar o sistema em produção
REM ============================================

echo.
echo ============================================
echo Executando database SQL para producao
echo ============================================
echo.

REM ============================================
REM Configuracao do banco de dados via variaveis de ambiente
REM 
REM Opcao 1: Carrega config.bat se existir (recomendado)
REM Opcao 2: Defina as variaveis manualmente antes de executar
REM 
REM OU use autenticacao integrada do Windows (deixe DB_USER e DB_PASSWORD vazios)
REM ============================================

REM Tenta carregar config.bat se existir
if exist "%~dp0config.bat" (
    call "%~dp0config.bat"
    echo Arquivo config.bat carregado.
    echo.
)

REM Verifica se as variaveis de ambiente estao definidas
if "%DB_SERVER%"=="" (
    echo ERRO: Variavel de ambiente DB_SERVER nao definida!
    echo Defina antes de executar: set DB_SERVER=servidor
    pause
    exit /b 1
)

if "%DB_NAME%"=="" (
    echo ERRO: Variavel de ambiente DB_NAME nao definida!
    echo Defina antes de executar: set DB_NAME=banco_de_dados
    pause
    exit /b 1
)

REM Define variaveis locais a partir das variaveis de ambiente
set SERVER=%DB_SERVER%
set DATABASE=%DB_NAME%
set USER=%DB_USER%
set PASSWORD=%DB_PASSWORD%

REM Detecta se deve usar autenticacao integrada (Windows Authentication)
set USE_WINDOWS_AUTH=0
if "%DB_USER%"=="" set USE_WINDOWS_AUTH=1
if "%DB_PASSWORD%"=="" set USE_WINDOWS_AUTH=1

echo Configuracao do banco de dados:
echo   Servidor: %SERVER%
echo   Banco: %DATABASE%
if %USE_WINDOWS_AUTH%==1 (
    echo   Autenticacao: Windows (Integrada)
) else (
    echo   Usuario: %USER%
)
echo.

REM Verifica se o banco existe e cria se necessario
echo Verificando se o banco de dados existe...
if %USE_WINDOWS_AUTH%==1 (
    sqlcmd -S %SERVER% -E -Q "IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = '%DATABASE%') CREATE DATABASE [%DATABASE%]" >nul 2>&1
) else (
    sqlcmd -S %SERVER% -U %USER% -P %PASSWORD% -Q "IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = '%DATABASE%') CREATE DATABASE [%DATABASE%]" >nul 2>&1
)
if errorlevel 1 (
    echo AVISO: Nao foi possivel criar o banco de dados automaticamente.
    echo Por favor, crie manualmente antes de continuar.
    echo.
) else (
    echo Banco de dados verificado/criado com sucesso.
    echo.
)

set /p CONFIRMAR="Deseja continuar? (S/N): "
if /i not "%CONFIRMAR%"=="S" (
    echo Operacao cancelada.
    pause
    exit /b
)

echo.
echo Executando scripts de criacao de tabelas...
echo.

REM Executa scripts de tabelas (001-011) na pasta tabelas/
for %%f in (tabelas\001_*.sql tabelas\002_*.sql tabelas\003_*.sql tabelas\004_*.sql tabelas\005_*.sql tabelas\006_*.sql tabelas\007_*.sql tabelas\008_*.sql tabelas\009_*.sql tabelas\010_*.sql tabelas\011_*.sql) do (
    echo Executando: %%f
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%%f" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%%f" -b
    )
    if errorlevel 1 (
        echo ERRO ao executar %%f
        pause
        exit /b 1
    )
    echo OK: %%f
    echo.
)

echo.
echo Executando scripts de criacao de views Power BI principais...
echo.

REM Executa scripts de views principais (011-018) na pasta views/
for %%f in (views\011_*.sql views\012_*.sql views\013_*.sql views\014_*.sql views\015_*.sql views\016_*.sql views\017_*.sql views\018_*.sql) do (
    echo Executando: %%f
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%%f" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%%f" -b
    )
    if errorlevel 1 (
        echo ERRO ao executar %%f
        pause
        exit /b 1
    )
    echo OK: %%f
    echo.
)

echo.
echo Executando scripts de criacao de views de dimensoes...
echo.

REM Executa scripts de views de dimensões (019-023, 027) na pasta views-dimensao/
for %%f in (views-dimensao\019_*.sql views-dimensao\020_*.sql views-dimensao\021_*.sql views-dimensao\022_*.sql views-dimensao\023_*.sql views-dimensao\027_*.sql) do (
    echo Executando: %%f
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%%f" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%%f" -b
    )
    if errorlevel 1 (
        echo ERRO ao executar %%f
        pause
        exit /b 1
    )
    echo OK: %%f
    echo.
)

echo.
echo Executando scripts de validacao...
echo.

REM Executa script de validação de views de dimensão (025)
set VALIDACAO_SCRIPT=validacao\025_validar_views_dimensao.sql
if exist "%VALIDACAO_SCRIPT%" (
    echo Executando: %VALIDACAO_SCRIPT%
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%VALIDACAO_SCRIPT%" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%VALIDACAO_SCRIPT%" -b
    )
    if errorlevel 1 (
        echo.
        echo AVISO: Erro ao executar %VALIDACAO_SCRIPT% (pode ser ignorado se views ainda estao vazias)
        echo.
    ) else (
        echo OK: %VALIDACAO_SCRIPT%
        echo.
    )
)

REM Executa script de validação de tipo destroy_user_id (026)
set VALIDACAO_SCRIPT=validacao\026_validar_tipo_destroy_user_id.sql
if exist "%VALIDACAO_SCRIPT%" (
    echo Executando: %VALIDACAO_SCRIPT%
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%VALIDACAO_SCRIPT%" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%VALIDACAO_SCRIPT%" -b
    )
    if errorlevel 1 (
        echo.
        echo AVISO: Erro ao executar %VALIDACAO_SCRIPT% (pode ser ignorado se tabelas ainda estao vazias)
        echo.
    ) else (
        echo OK: %VALIDACAO_SCRIPT%
        echo.
    )
)

echo.
echo Executando script de configuracao de permissoes...
echo.

REM Executa script de segurança (024)
set SEGURANCA_SCRIPT=seguranca\024_configurar_permissoes_usuario.sql
if exist "%SEGURANCA_SCRIPT%" (
    echo Executando: %SEGURANCA_SCRIPT%
    if %USE_WINDOWS_AUTH%==1 (
        sqlcmd -S %SERVER% -d %DATABASE% -E -i "%SEGURANCA_SCRIPT%" -b
    ) else (
        sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%SEGURANCA_SCRIPT%" -b
    )
    if errorlevel 1 (
        echo.
        echo AVISO: Erro ao executar %SEGURANCA_SCRIPT%
        echo.
    ) else (
        echo OK: %SEGURANCA_SCRIPT%
        echo.
    )
)

echo.
echo ============================================
echo Todas as scripts de database foram executados!
echo ============================================
echo.
echo RESUMO:
echo   - Tabelas: Criadas (001-011)
echo   - Views Power BI: Criadas (011-018)
echo   - Views Dimensao: Criadas (019-023, 027)
echo   - Validacao: Executada (025, 026)
echo   - Seguranca: Executado (024)
echo.
echo NOTA: O script de seguranca (024) configura permissoes para o usuario do config.bat.
echo        Em desenvolvimento com 'sa', as permissoes nao sao necessarias (ja e sysadmin).
echo.

pause
