@echo off
title Exibir Ajuda e Comandos Disponíveis
@REM ================================================================
@REM Script: 06-exibir_ajuda.bat
@REM Finalidade:
@REM   Exibe a lista de comandos e instruções de uso disponíveis.
@REM
@REM Uso:
@REM   06-exibir_ajuda.bat
@REM
@REM Exemplo:
@REM   Clique duas vezes no arquivo ou execute no terminal.
@REM ================================================================
setlocal
cd /d "%~dp0"
java -jar "target\extrator.jar" --ajuda
pause