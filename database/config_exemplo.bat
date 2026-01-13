@echo off
REM ============================================
REM Exemplo de configuracao de variaveis de ambiente
REM Copie este arquivo, renomeie para config.bat e configure com suas credenciais
REM NUNCA commite o arquivo config.bat no Git!
REM ============================================

REM Opcao 1: Autenticacao SQL Server
set DB_SERVER=localhost
set DB_NAME=eslcloud
set DB_USER=sa
set DB_PASSWORD=SqlDocker!2025

REM Opcao 2: Autenticacao Integrada do Windows (deixe DB_USER e DB_PASSWORD vazios)
REM set DB_SERVER=localhost
REM set DB_NAME=seu_banco_de_dados
