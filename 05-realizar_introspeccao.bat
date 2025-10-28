@echo off
title Realizar Introspecção GraphQL
@REM ================================================================
@REM Script: 05-realizar_introspeccao.bat
@REM Finalidade:
@REM   Executa a introspecção da API GraphQL para mapear o esquema.
@REM
@REM Uso:
@REM   05-realizar_introspeccao.bat
@REM
@REM Exemplo:
@REM   Clique duas vezes no arquivo ou execute no terminal.
@REM ================================================================
setlocal
cd /d "%~dp0"
java -jar "target\extrator.jar" --introspeccao
pause