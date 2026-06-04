@echo off
echo Iniciando sincronizacao de usuarios...
pushd ..
java -jar target\extrator.jar --sincronizar-usuarios
if %ERRORLEVEL% neq 0 (
    echo [ERRO] Falha na sincronizacao. O Java retornou um erro de execucao.
    popd
    pause
    exit /b %ERRORLEVEL%
)
echo Sincronizacao concluida com sucesso.
popd
pause
