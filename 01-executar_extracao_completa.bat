@echo off
title Executar Extração Completa - Fluxo Principal (ETL)
@REM ================================================================
@REM Script: 01-executar_extracao_completa.bat
@REM Finalidade:
@REM   Executa o fluxo completo de extração (ETL) sem argumentos.
@REM   A Main utiliza '--fluxo-completo' como padrão quando nenhum
@REM   argumento é informado.
@REM
@REM Uso:
@REM   01-executar_extracao_completa.bat
@REM
@REM Exemplo:
@REM   Clique duas vezes no arquivo ou execute no terminal.
@REM ================================================================
setlocal
cd /d "%~dp0"
java -jar "target\extrator.jar"
pause