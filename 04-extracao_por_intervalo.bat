@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

REM ================================================================
REM Script: 04-extracao_por_intervalo.bat
REM Finalidade:
REM   Executa a extracao de dados por intervalo de datas.
REM   Permite extrair dados de um periodo especifico (ex: 2024-11-01 a 2025-03-31).
REM   O sistema divide automaticamente em blocos de 30 dias e valida
REM   regras de limitacao de tempo (cada bloco de 30 dias = sem limite de horas).
REM   Permite escolher API e entidade especifica.
REM
REM Uso:
REM   04-extracao_por_intervalo.bat
REM   04-extracao_por_intervalo.bat YYYY-MM-DD YYYY-MM-DD
REM   04-extracao_por_intervalo.bat YYYY-MM-DD YYYY-MM-DD api
REM   04-extracao_por_intervalo.bat YYYY-MM-DD YYYY-MM-DD api entidade
REM
REM Exemplos:
REM   04-extracao_por_intervalo.bat 2024-10-26 2024-12-26
REM   04-extracao_por_intervalo.bat 2024-10-26 2024-12-26 dataexport
REM   04-extracao_por_intervalo.bat 2024-10-26 2024-12-26 dataexport localizacao_cargas
REM
REM Funcionalidades:
REM   - Aceita parametros na linha de comando OU menu interativo
REM   - Permite escolher API especifica (GraphQL ou DataExport)
REM   - Permite escolher entidade especifica
REM   - Divide periodo em blocos de 30 dias automaticamente
REM   - Cada bloco de 30 dias e tratado como "< 31 dias" (sem limite de horas)
REM   - Executa extracao para cada bloco sequencialmente
REM   - Gera logs detalhados
REM ================================================================

REM Verificar se parametros foram passados na linha de comando
if "%~1"=="" (
    REM Modo interativo - sem parametros
    goto :MODO_INTERATIVO
)

REM Modo com parametros - usar valores da linha de comando
set "DATA_INICIO=%~1"
set "DATA_FIM=%~2"
set "API_ESCOLHIDA=%~3"
set "ENTIDADE_ESCOLHIDA=%~4"

REM Validar que pelo menos as datas foram fornecidas
if "%DATA_INICIO%"=="" (
    echo ERRO: Data de inicio nao informada!
    echo.
    echo Uso: 04-extracao_por_intervalo.bat [DATA_INICIO] [DATA_FIM] [API] [ENTIDADE]
    echo Exemplo: 04-extracao_por_intervalo.bat 2024-10-26 2024-12-26 dataexport localizacao_cargas
    pause
    exit /b 1
)

if "%DATA_FIM%"=="" (
    echo ERRO: Data de fim nao informada!
    echo.
    echo Uso: 04-extracao_por_intervalo.bat [DATA_INICIO] [DATA_FIM] [API] [ENTIDADE]
    echo Exemplo: 04-extracao_por_intervalo.bat 2024-10-26 2024-12-26 dataexport localizacao_cargas
    pause
    exit /b 1
)

REM Validar formato básico das datas
REM Validação completa será feita pelo Java ao parsear a data
REM Verificar se contém hífens usando substituição de string
set "TEMP_INICIO=%DATA_INICIO:-=%"
if "%TEMP_INICIO%"=="%DATA_INICIO%" (
    echo ERRO: Data de inicio deve estar no formato YYYY-MM-DD
    echo Valor recebido: %DATA_INICIO%
    pause
    exit /b 1
)

set "TEMP_FIM=%DATA_FIM:-=%"
if "%TEMP_FIM%"=="%DATA_FIM%" (
    echo ERRO: Data de fim deve estar no formato YYYY-MM-DD
    echo Valor recebido: %DATA_FIM%
    pause
    exit /b 1
)

REM Variáveis já estão definidas, continuar

REM Pular para confirmacao
goto :CONFIRMACAO

:MODO_INTERATIVO
echo ================================================================
echo EXTRACAO POR INTERVALO DE DATAS
echo ================================================================
echo.
echo Este script permite extrair dados de um periodo especifico.
echo O sistema dividira automaticamente em blocos de 31 dias.
echo.
echo Formato de data: YYYY-MM-DD (exemplo: 2024-11-01)
echo.

REM Solicitar data de inicio
set /p DATA_INICIO="Digite a data de inicio (YYYY-MM-DD): "
if "%DATA_INICIO%"=="" (
    echo ERRO: Data de inicio nao informada!
    pause
    exit /b 1
)

REM Validar formato basico (deve ter 10 caracteres e conter hifens)
echo %DATA_INICIO% | findstr /R "^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$" >nul
if errorlevel 1 (
    echo ERRO: Formato de data invalido! Use YYYY-MM-DD (exemplo: 2024-11-01)
    pause
    exit /b 1
)

REM Solicitar data de fim
set /p DATA_FIM="Digite a data de fim (YYYY-MM-DD): "
if "%DATA_FIM%"=="" (
    echo ERRO: Data de fim nao informada!
    pause
    exit /b 1
)

REM Validar formato basico
echo %DATA_FIM% | findstr /R "^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$" >nul
if errorlevel 1 (
    echo ERRO: Formato de data invalido! Use YYYY-MM-DD (exemplo: 2025-03-31)
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ESCOLHA DE API E ENTIDADE
echo ================================================================
echo.
echo Escolha uma opcao:
echo.
echo   1. Extrair TODAS as APIs e entidades
echo   2. Extrair API especifica (GraphQL ou DataExport)
echo   0. Cancelar
echo.
set /p OPCAO_API="Digite o numero da opcao: "

if "%OPCAO_API%"=="0" (
    echo Operacao cancelada.
    exit /b 0
)

set "API_ESCOLHIDA="
set "ENTIDADE_ESCOLHIDA="

if "%OPCAO_API%"=="2" (
    echo.
    echo ================================================================
    echo   ESCOLHA DA API
    echo ================================================================
    echo.
    echo   1. GraphQL (Coletas, Fretes, Faturas GraphQL)
    echo   2. DataExport (Manifestos, Cotacoes, Localizacao de Cargas, Contas a Pagar, Faturas por Cliente)
    echo   0. Voltar
    echo.
    set /p API_NUM="Digite o numero da API: "
    
    if "!API_NUM!"=="0" (
        echo Voltando...
        goto :END
    )
    
    if "!API_NUM!"=="1" (
        set "API_ESCOLHIDA=graphql"
    ) else if "!API_NUM!"=="2" (
        set "API_ESCOLHIDA=dataexport"
    ) else (
        echo ERRO: Opcao invalida!
        pause
        exit /b 1
    )
    
    echo.
    echo ================================================================
    echo   ESCOLHA DA ENTIDADE
    echo ================================================================
    echo.
    echo   0. Extrair TODAS as entidades da API escolhida
    echo.
    
    if "!API_ESCOLHIDA!"=="graphql" (
        echo   Entidades GraphQL:
        echo   1. coletas
        echo   2. fretes
        echo   3. faturas_graphql
        echo   4. usuarios_sistema
        echo.
        set /p ENTIDADE_NUM="Digite o numero da entidade (0 = todas): "
        
        if "!ENTIDADE_NUM!"=="0" (
            set "ENTIDADE_ESCOLHIDA="
        ) else if "!ENTIDADE_NUM!"=="1" (
            set "ENTIDADE_ESCOLHIDA=coletas"
        ) else if "!ENTIDADE_NUM!"=="2" (
            set "ENTIDADE_ESCOLHIDA=fretes"
        ) else if "!ENTIDADE_NUM!"=="3" (
            set "ENTIDADE_ESCOLHIDA=faturas_graphql"
        ) else if "!ENTIDADE_NUM!"=="4" (
            set "ENTIDADE_ESCOLHIDA=usuarios_sistema"
        ) else (
            echo ERRO: Numero invalido!
            pause
            exit /b 1
        )
    ) else if "!API_ESCOLHIDA!"=="dataexport" (
        echo   Entidades DataExport:
        echo   1. manifestos
        echo   2. cotacoes
        echo   3. localizacao_cargas
        echo   4. contas_a_pagar
        echo   5. faturas_por_cliente
        echo.
        set /p ENTIDADE_NUM="Digite o numero da entidade (0 = todas): "
        
        if "!ENTIDADE_NUM!"=="0" (
            set "ENTIDADE_ESCOLHIDA="
        ) else if "!ENTIDADE_NUM!"=="1" (
            set "ENTIDADE_ESCOLHIDA=manifestos"
        ) else if "!ENTIDADE_NUM!"=="2" (
            set "ENTIDADE_ESCOLHIDA=cotacoes"
        ) else if "!ENTIDADE_NUM!"=="3" (
            set "ENTIDADE_ESCOLHIDA=localizacao_cargas"
        ) else if "!ENTIDADE_NUM!"=="4" (
            set "ENTIDADE_ESCOLHIDA=contas_a_pagar"
        ) else if "!ENTIDADE_NUM!"=="5" (
            set "ENTIDADE_ESCOLHIDA=faturas_por_cliente"
        ) else (
            echo ERRO: Numero invalido!
            pause
            exit /b 1
        )
    )
)

:CONFIRMACAO
echo.
echo ================================================================
echo Confirmacao
echo ================================================================
echo Data de Inicio: %DATA_INICIO%
echo Data de Fim: %DATA_FIM%
if not "%API_ESCOLHIDA%"=="" (
    echo API: %API_ESCOLHIDA%
    if not "%ENTIDADE_ESCOLHIDA%"=="" (
        echo Entidade: %ENTIDADE_ESCOLHIDA%
    ) else (
        echo Entidade: TODAS
    )
) else (
    echo API: TODAS
    echo Entidade: TODAS
)
echo.

REM Se parametros foram passados, pular confirmacao interativa
if not "%~1"=="" (
    echo Parametros recebidos via linha de comando. Iniciando extracao...
    goto :COMPILAR
)

set /p CONFIRMA="Confirma a extracao para este periodo? (S/N): "
if /i not "%CONFIRMA%"=="S" (
    echo Operacao cancelada pelo usuario.
    pause
    exit /b 0
)

:COMPILAR
echo.
echo ================================================================
echo Compilando projeto...
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

echo.
echo ================================================================
echo Iniciando extracao por intervalo
echo ================================================================
echo Periodo: %DATA_INICIO% a %DATA_FIM%
if not "%API_ESCOLHIDA%"=="" (
    echo API: %API_ESCOLHIDA%
    if not "%ENTIDADE_ESCOLHIDA%"=="" (
        echo Entidade: %ENTIDADE_ESCOLHIDA%
    ) else (
        echo Entidade: TODAS
    )
) else (
    echo API: TODAS
    echo Entidade: TODAS
)
echo.
echo ATENCAO: Este processo pode demorar varios minutos...
echo O sistema dividira automaticamente em blocos de 30 dias (sem limite de horas).
echo.
echo.

REM Construir comando com parâmetros opcionais usando delayed expansion
set "CMD_ARGS=!DATA_INICIO! !DATA_FIM!"
if not "!API_ESCOLHIDA!"=="" (
    set "CMD_ARGS=!CMD_ARGS! !API_ESCOLHIDA!"
    if not "!ENTIDADE_ESCOLHIDA!"=="" (
        set "CMD_ARGS=!CMD_ARGS! !ENTIDADE_ESCOLHIDA!"
    )
)

REM Executar comando
java -jar "target\extrator.jar" --extracao-intervalo !CMD_ARGS!

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================================
    echo EXTRACAO POR INTERVALO CONCLUIDA COM SUCESSO!
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo EXTRACAO FALHOU ^(Exit Code: %ERRORLEVEL%^)
    echo ================================================================
)

:END
echo.
echo Verifique os logs na pasta 'logs' para mais detalhes.
echo.
pause
endlocal
