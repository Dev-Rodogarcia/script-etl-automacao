@echo off
title Validar Acesso às Credenciais e APIs
@REM ================================================================
@REM Script: 04-validar_acesso.bat
@REM Finalidade:
@REM   Valida acesso e credenciais necessárias para consumo das APIs.
@REM
@REM Uso:
@REM   04-validar_acesso.bat
@REM
@REM Exemplo:
@REM   Clique duas vezes no arquivo ou execute no terminal.
@REM ================================================================
setlocal
cd /d "%~dp0"
java -jar "target\extrator.jar" --validar
pause