@echo off
title Compilar Projeto ESL Cloud - Gerar extrator.jar
@REM ================================================================
@REM Script: 00-compilar_projeto.bat
@REM Finalidade:
@REM   Compila o projeto, limpa artefatos antigos e gera o arquivo
@REM   final 'target\extrator.jar' para execução dos demais scripts.
@REM
@REM Uso:
@REM   00-compilar_projeto.bat
@REM
@REM Exemplo:
@REM   Clique duas vezes no arquivo ou execute no terminal.
@REM
@REM Observações:
@REM   - Requer Maven instalado e configurado no PATH.
@REM   - Gera sempre 'target\extrator.jar' (padronizado via pom.xml).
@REM ================================================================
setlocal
cd /d "%~dp0"
echo [INFO] Limpando e compilando o projeto com Maven...
mvn clean package
echo [INFO] Concluído.
pause