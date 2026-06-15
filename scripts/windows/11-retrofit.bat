@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /i not "%EXTRATOR_SKIP_CHCP%"=="1" chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%.") do set "SCRIPT_DIR=%%~fI"
for %%I in ("%SCRIPT_DIR%\..\..") do set "REPO_ROOT=%%~fI"

set "JAR_PATH=%REPO_ROOT%\target\extrator.jar"
set "ENV_FILE=%REPO_ROOT%\.env"
set "RETROFIT_CONFIG_FILE=%REPO_ROOT%\database\config.bat"
set "SAFETY_SCRIPT=%SCRIPT_DIR%\verificar_execucao_ativa.ps1"
set "FINAL_EXIT_CODE=1"

pushd "%REPO_ROOT%"

echo ================================================================
echo RETROFIT HISTORICO POR ENTIDADE
echo ================================================================
echo Esta rotina executa carga aditiva em modo Retrofit.
echo O prune de ausentes permanece desabilitado pelo Java.
echo O Daemon deve estar parado durante toda a execucao.
echo ================================================================
echo.

call :ENSURE_JAVA
if errorlevel 1 goto :END

if not exist "%JAR_PATH%" (
    echo [ERRO] JAR de producao nao encontrado:
    echo   %JAR_PATH%
    goto :END
)

if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo de ambiente aprovado nao encontrado:
    echo   %ENV_FILE%
    goto :END
)

if not exist "%RETROFIT_CONFIG_FILE%" (
    echo [ERRO] Configuracao SQLCMD nao encontrada:
    echo   %RETROFIT_CONFIG_FILE%
    goto :END
)

call :RETROFIT_SAFETY_GATE
if errorlevel 1 goto :END

if /i not "%EXTRATOR_SKIP_AUTH_CHECK%"=="1" (
    call :ISOLATE_ENVIRONMENT
    echo Autenticacao obrigatoria para executar o Retrofit.
    java --enable-native-access=ALL-UNNAMED "-DETL_BASE_DIR=%REPO_ROOT%" "-Detl.base.dir=%REPO_ROOT%" "-Dextrator.env.file=%ENV_FILE%" -jar "%JAR_PATH%" --auth-check RUN_EXTRACAO_INTERVALO "Executar Retrofit historico"
    if errorlevel 1 (
        echo [ERRO] Acesso negado.
        goto :END
    )
)

call :SELECT_ENTITY
if errorlevel 1 (
    set "FINAL_EXIT_CODE=3"
    goto :END
)

call :READ_WINDOW_DAYS
if errorlevel 1 (
    set "FINAL_EXIT_CODE=3"
    goto :END
)

call :LOAD_EXPECTED_DATABASE_TARGET
if errorlevel 1 goto :END

call :VALIDATE_APPROVED_ENVIRONMENT
if errorlevel 1 goto :END

for /f "tokens=1,2 delims=|" %%A in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "$days=[int]$env:RETROFIT_WINDOW_DAYS; $end=(Get-Date).Date; $start=$end.AddDays(-$days); Write-Output ($start.ToString('yyyy-MM-dd') + '|' + $end.ToString('yyyy-MM-dd'))"') do (
    set "DATA_INICIO=%%A"
    set "DATA_FIM=%%B"
)

if not defined DATA_INICIO (
    echo [ERRO] Nao foi possivel calcular a data inicial do Retrofit.
    goto :END
)
if not defined DATA_FIM (
    echo [ERRO] Nao foi possivel calcular a data final do Retrofit.
    goto :END
)

echo.
echo ================================================================
echo CONFIRMACAO DO RETROFIT
echo ================================================================
echo Entidade: !RETROFIT_ENTITY!
echo API: !RETROFIT_API!
echo Janela informada: !RETROFIT_WINDOW_DAYS! dia^(s^)
echo Periodo calculado: !DATA_INICIO! a !DATA_FIM!
echo Banco confirmado: !EXPECTED_DB_SERVER_DISPLAY! ^| !EXPECTED_DB_NAME!
echo Modo: RETROFIT ^(carga aditiva, sem prune de ausentes^)
echo ================================================================
echo.
set "RETROFIT_CONFIRM="
set /p "RETROFIT_CONFIRM=Digite RETROFIT para confirmar ou pressione ENTER para cancelar: " || (
    set "FINAL_EXIT_CODE=3"
    goto :END
)
if /i not "!RETROFIT_CONFIRM!"=="RETROFIT" (
    echo.
    echo [INFO] Retrofit cancelado pelo operador.
    set "FINAL_EXIT_CODE=3"
    goto :END
)

echo.
echo Revalidando concorrencia imediatamente antes da execucao...
call :RETROFIT_SAFETY_GATE
if errorlevel 1 goto :END

call :ISOLATE_ENVIRONMENT

echo.
echo ================================================================
echo EXECUTANDO RETROFIT
echo ================================================================
echo Entidade: !RETROFIT_ENTITY!
echo Periodo: !DATA_INICIO! a !DATA_FIM!
echo ================================================================
echo.

java --enable-native-access=ALL-UNNAMED "-DETL_BASE_DIR=%REPO_ROOT%" "-Detl.base.dir=%REPO_ROOT%" "-Dextrator.env.file=%ENV_FILE%" -jar "%JAR_PATH%" --extracao-intervalo "!DATA_INICIO!" "!DATA_FIM!" "!RETROFIT_ENTITY!" --retrofit
set "FINAL_EXIT_CODE=!ERRORLEVEL!"

echo.
if "!FINAL_EXIT_CODE!"=="0" (
    echo [OK] Retrofit concluido com sucesso.
) else if "!FINAL_EXIT_CODE!"=="2" (
    echo [AVISO] Retrofit concluido com falhas parciais.
) else (
    echo [ERRO] Retrofit falhou com codigo !FINAL_EXIT_CODE!.
)
goto :END

:SELECT_ENTITY
echo ================================================================
echo SELECIONE A ENTIDADE
echo ================================================================
echo 01. coletas
echo 02. fretes
echo 03. usuarios_sistema
echo 04. manifestos
echo 05. cotacoes
echo 06. localizacao_cargas
echo 07. contas_a_pagar
echo 08. faturas_por_cliente
echo 09. inventario
echo 10. sinistros
echo 11. raster_viagens
echo 00. Cancelar
echo.
set "ENTITY_OPTION="
set /p "ENTITY_OPTION=Escolha uma entidade [1-11, 0=Cancelar]: " || exit /b 1
set "ENTITY_OPTION=!ENTITY_OPTION: =!"
if "!ENTITY_OPTION!"=="00" set "ENTITY_OPTION=0"
if "!ENTITY_OPTION:~0,1!"=="0" if not "!ENTITY_OPTION!"=="0" set "ENTITY_OPTION=!ENTITY_OPTION:~1!"

if "!ENTITY_OPTION!"=="0" exit /b 1
if "!ENTITY_OPTION!"=="1" (
    set "RETROFIT_ENTITY=coletas"
    set "RETROFIT_API=graphql"
    exit /b 0
)
if "!ENTITY_OPTION!"=="2" (
    set "RETROFIT_ENTITY=fretes"
    set "RETROFIT_API=graphql"
    exit /b 0
)
if "!ENTITY_OPTION!"=="3" (
    set "RETROFIT_ENTITY=usuarios_sistema"
    set "RETROFIT_API=graphql"
    exit /b 0
)
if "!ENTITY_OPTION!"=="4" (
    set "RETROFIT_ENTITY=manifestos"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="5" (
    set "RETROFIT_ENTITY=cotacoes"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="6" (
    set "RETROFIT_ENTITY=localizacao_cargas"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="7" (
    set "RETROFIT_ENTITY=contas_a_pagar"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="8" (
    set "RETROFIT_ENTITY=faturas_por_cliente"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="9" (
    set "RETROFIT_ENTITY=inventario"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="10" (
    set "RETROFIT_ENTITY=sinistros"
    set "RETROFIT_API=dataexport"
    exit /b 0
)
if "!ENTITY_OPTION!"=="11" (
    set "RETROFIT_ENTITY=raster_viagens"
    set "RETROFIT_API=raster"
    exit /b 0
)

echo.
echo [ERRO] Entidade invalida.
echo.
goto :SELECT_ENTITY

:READ_WINDOW_DAYS
echo.
echo ================================================================
echo JANELA HISTORICA
echo ================================================================
echo Informe quantos dias retroativos devem ser processados.
echo Limite operacional: 1 a 3650 dias.
echo A data final sera a data atual do servidor.
echo.
set "RETROFIT_WINDOW_DAYS="
set /p "RETROFIT_WINDOW_DAYS=Janela em dias [1-3650, ENTER=Cancelar]: " || exit /b 1
set "RETROFIT_WINDOW_DAYS=!RETROFIT_WINDOW_DAYS: =!"
if not defined RETROFIT_WINDOW_DAYS exit /b 1

powershell -NoProfile -ExecutionPolicy Bypass -Command "$value=0; if ([int]::TryParse($env:RETROFIT_WINDOW_DAYS, [ref]$value) -and $value -ge 1 -and $value -le 3650) { exit 0 }; exit 1"
if errorlevel 1 (
    echo.
    echo [ERRO] Janela invalida. Informe um numero inteiro entre 1 e 3650.
    goto :READ_WINDOW_DAYS
)
exit /b 0

:LOAD_EXPECTED_DATABASE_TARGET
set "CFG_DB_SERVER="
set "CFG_DB_PORT="
set "CFG_DB_NAME="
setlocal DisableDelayedExpansion
set "DB_SERVER="
set "DB_PORT="
set "DB_NAME="
set "DB_USER="
set "DB_PASSWORD="
call "%RETROFIT_CONFIG_FILE%" >nul
set "CONFIG_EXIT=%ERRORLEVEL%"
endlocal & set "CONFIG_EXIT=%CONFIG_EXIT%" & set "CFG_DB_SERVER=%DB_SERVER%" & set "CFG_DB_PORT=%DB_PORT%" & set "CFG_DB_NAME=%DB_NAME%"

if not "!CONFIG_EXIT!"=="0" (
    echo [ERRO] database\config.bat retornou codigo !CONFIG_EXIT!.
    exit /b 1
)
if not defined CFG_DB_SERVER (
    echo [ERRO] DB_SERVER nao definido em database\config.bat.
    exit /b 1
)
if not defined CFG_DB_NAME (
    echo [ERRO] DB_NAME nao definido em database\config.bat.
    exit /b 1
)

set "RETROFIT_EXPECTED_DB_SERVER=!CFG_DB_SERVER!"
set "RETROFIT_EXPECTED_DB_PORT=!CFG_DB_PORT!"
set "RETROFIT_EXPECTED_DB_NAME=!CFG_DB_NAME!"
set "EXPECTED_DB_NAME=!CFG_DB_NAME!"
set "EXPECTED_DB_SERVER_DISPLAY=!CFG_DB_SERVER!"
if defined CFG_DB_PORT set "EXPECTED_DB_SERVER_DISPLAY=!EXPECTED_DB_SERVER_DISPLAY!:!CFG_DB_PORT!"
exit /b 0

:VALIDATE_APPROVED_ENVIRONMENT
set "RETROFIT_ENV_FILE=%ENV_FILE%"
set "PS_VALIDATE=$ErrorActionPreference='Stop';"
set "PS_VALIDATE=!PS_VALIDATE! function Read-DotEnv([string]$Path) { $result=@{}; foreach($line in Get-Content -LiteralPath $Path -Encoding UTF8) { $text=$line.Trim(); if($text.Length -eq 0 -or $text.StartsWith('#')) { continue }; if($text.StartsWith('export ')) { $text=$text.Substring(7).Trim() }; $separator=$text.IndexOf('='); if($separator -le 0) { continue }; $key=$text.Substring(0,$separator).Trim(); $value=$text.Substring($separator+1).Trim(); if($value.Length -ge 2 -and (($value.StartsWith([char]34) -and $value.EndsWith([char]34)) -or ($value.StartsWith([char]39) -and $value.EndsWith([char]39)))) { $value=$value.Substring(1,$value.Length-2) }; $result[$key]=$value }; return $result };"
set "PS_VALIDATE=!PS_VALIDATE! function Require-Value($Config,[string]$Key) { if(-not $Config.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace([string]$Config[$Key])) { throw ('Configuracao obrigatoria ausente no .env: ' + $Key) }; return ([string]$Config[$Key]).Trim() };"
set "PS_VALIDATE=!PS_VALIDATE! function Normalize-Host([string]$HostName) { $value=$HostName.Trim().ToLowerInvariant(); if($value.StartsWith('tcp:')) { $value=$value.Substring(4) }; if(@('localhost','127.0.0.1','::1','(local)','.') -contains $value) { return 'localhost' }; return $value };"
REM O bootstrap atual instancia os clientes GraphQL e DataExport de forma eager.
REM Por isso os dois tokens sao obrigatorios mesmo em Retrofit de entidade unica.
set "PS_VALIDATE=!PS_VALIDATE! $cfg=Read-DotEnv $env:RETROFIT_ENV_FILE; $dbUrl=Require-Value $cfg 'DB_URL'; [void](Require-Value $cfg 'DB_USER'); [void](Require-Value $cfg 'DB_PASSWORD'); [void](Require-Value $cfg 'API_GRAPHQL_TOKEN'); [void](Require-Value $cfg 'API_DATAEXPORT_TOKEN');"
set "PS_VALIDATE=!PS_VALIDATE! $basePrimary=if($cfg.ContainsKey('API_BASEURL')){[string]$cfg['API_BASEURL']}else{''}; $baseAlias=if($cfg.ContainsKey('API_BASE_URL')){[string]$cfg['API_BASE_URL']}else{''}; if([string]::IsNullOrWhiteSpace($basePrimary) -and [string]::IsNullOrWhiteSpace($baseAlias)){throw 'Configuracao obrigatoria ausente no .env: API_BASEURL'}; if(-not [string]::IsNullOrWhiteSpace($basePrimary) -and -not [string]::IsNullOrWhiteSpace($baseAlias) -and $basePrimary.TrimEnd('/') -ine $baseAlias.TrimEnd('/')){throw 'API_BASEURL e API_BASE_URL apontam para destinos diferentes'};"
set "PS_VALIDATE=!PS_VALIDATE! if($env:RETROFIT_API -eq 'raster'){ [void](Require-Value $cfg 'RASTER_LOGIN'); $rasterSenha=if($cfg.ContainsKey('RASTER_SENHA')){[string]$cfg['RASTER_SENHA']}else{''}; $rasterPassword=if($cfg.ContainsKey('RASTER_PASSWORD')){[string]$cfg['RASTER_PASSWORD']}else{''}; if([string]::IsNullOrWhiteSpace($rasterSenha) -and [string]::IsNullOrWhiteSpace($rasterPassword)){throw 'Configuracao obrigatoria ausente no .env: RASTER_SENHA ou RASTER_PASSWORD'}; if(-not [string]::IsNullOrWhiteSpace($rasterSenha) -and -not [string]::IsNullOrWhiteSpace($rasterPassword) -and $rasterSenha -cne $rasterPassword){throw 'RASTER_SENHA e RASTER_PASSWORD possuem valores divergentes'}; if($cfg.ContainsKey('RASTER_ENABLED') -and ([string]$cfg['RASTER_ENABLED']).Trim() -ieq 'false'){throw 'RASTER_ENABLED=false bloqueia o Retrofit da entidade Raster'} };"
set "PS_VALIDATE=!PS_VALIDATE! $match=[regex]::Match($dbUrl,'\Ajdbc:sqlserver://(?<authority>.*?);(?<properties>.*)\z',[System.Text.RegularExpressions.RegexOptions]::IgnoreCase); if(-not $match.Success){throw 'DB_URL deve seguir jdbc:sqlserver://servidor:porta;databaseName=banco;'}; $authority=$match.Groups['authority'].Value.Trim(); $actualHost=$authority; $actualPort='1433'; if($authority -match '\A\[(?<host>.+?)\](?::(?<port>\d+))?\z'){ $actualHost=$Matches['host']; if($Matches['port']){$actualPort=$Matches['port']} } elseif($authority -match '\A(?<host>.+):(?<port>\d+)\z'){ $actualHost=$Matches['host']; $actualPort=$Matches['port'] };"
set "PS_VALIDATE=!PS_VALIDATE! $properties=@{}; foreach($segment in $match.Groups['properties'].Value.Split(';')){if([string]::IsNullOrWhiteSpace($segment)){continue}; $idx=$segment.IndexOf('='); if($idx -gt 0){$properties[$segment.Substring(0,$idx).Trim().ToLowerInvariant()]=$segment.Substring($idx+1).Trim()}}; $actualDatabase=if($properties.ContainsKey('databasename')){$properties['databasename']}elseif($properties.ContainsKey('database')){$properties['database']}else{''}; if([string]::IsNullOrWhiteSpace($actualDatabase)){throw 'DB_URL nao informa databaseName'};"
set "PS_VALIDATE=!PS_VALIDATE! $expectedHost=$env:RETROFIT_EXPECTED_DB_SERVER.Trim(); $expectedPort=$env:RETROFIT_EXPECTED_DB_PORT.Trim(); if($expectedHost -match '\A(?<host>.+),(?<port>\d+)\z'){ $expectedHost=$Matches['host']; if([string]::IsNullOrWhiteSpace($expectedPort)){$expectedPort=$Matches['port']} }; if((Normalize-Host $actualHost) -ine (Normalize-Host $expectedHost)){throw ('DB_URL aponta para servidor diferente de database/config.bat: ' + $actualHost + ' <> ' + $expectedHost)}; if(-not [string]::IsNullOrWhiteSpace($expectedPort) -and $actualPort -ne $expectedPort){throw ('DB_URL aponta para porta diferente de database/config.bat: ' + $actualPort + ' <> ' + $expectedPort)}; if($actualDatabase -ine $env:RETROFIT_EXPECTED_DB_NAME.Trim()){throw ('DB_URL aponta para banco diferente de database/config.bat: ' + $actualDatabase + ' <> ' + $env:RETROFIT_EXPECTED_DB_NAME)};"
set "PS_VALIDATE=!PS_VALIDATE! Write-Host '[OK] .env aprovado e credenciais obrigatorias presentes.'; Write-Host ('[OK] Destino confirmado: ' + $actualHost + ':' + $actualPort + ' | Banco: ' + $actualDatabase);"

powershell -NoProfile -ExecutionPolicy Bypass -Command "!PS_VALIDATE!"
set "VALIDATION_EXIT=!ERRORLEVEL!"
set "PS_VALIDATE="
if not "!VALIDATION_EXIT!"=="0" (
    echo.
    echo [BLOQUEIO] O ambiente do Retrofit nao foi aprovado.
    exit /b 1
)
exit /b 0

:ISOLATE_ENVIRONMENT
for /f "tokens=1 delims==" %%V in ('set API_ 2^>nul') do set "%%V="
for /f "tokens=1 delims==" %%V in ('set DB_ 2^>nul') do set "%%V="
for /f "tokens=1 delims==" %%V in ('set RASTER_ 2^>nul') do set "%%V="
set "JAVA_TOOL_OPTIONS="
set "_JAVA_OPTIONS="
set "JDK_JAVA_OPTIONS="
set "EXTRATOR_ALLOW_CONCURRENT_RUN="
set "EXTRATOR_ENV_FILE=%ENV_FILE%"
set "ETL_BASE_DIR=%REPO_ROOT%"
exit /b 0

:RETROFIT_SAFETY_GATE
if not exist "%SAFETY_SCRIPT%" (
    echo [ERRO] Verificador de execucao ativa nao encontrado:
    echo   %SAFETY_SCRIPT%
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%SAFETY_SCRIPT%" -RepoRoot "%REPO_ROOT%"
set "SAFETY_EXIT=!ERRORLEVEL!"
if "!SAFETY_EXIT!"=="0" (
    echo [OK] Daemon parado e nenhuma execucao concorrente detectada.
    exit /b 0
)
if "!SAFETY_EXIT!"=="2" (
    echo.
    echo [BLOQUEIO] Retrofit cancelado: existe execucao ativa.
    echo [INFO] Pare o Daemon pelo menu de producao e confirme o estado STOPPED.
    exit /b 1
)

echo.
echo [ERRO] Falha ao verificar execucoes ativas ^(codigo !SAFETY_EXIT!^).
exit /b 1

:ENSURE_JAVA
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        exit /b 0
    )
)

for %%V in (25 21 17) do (
    for /f "delims=" %%D in ('dir /b /ad /o-n "C:\Program Files\Eclipse Adoptium\jdk-%%V*" 2^>nul') do (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        exit /b 0
    )
)

where java.exe >nul 2>&1
if not errorlevel 1 exit /b 0

echo [ERRO] Java 17 ou superior nao encontrado.
exit /b 1

:END
if not defined FINAL_EXIT_CODE set "FINAL_EXIT_CODE=1"
popd
endlocal & exit /b %FINAL_EXIT_CODE%
