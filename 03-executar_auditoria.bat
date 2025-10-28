@echo off
title Executar Auditoria (padrão 24h ou período específico)
@REM ================================================================
@REM Script: 03-executar_auditoria.bat
@REM Finalidade:
@REM   Executa a auditoria do sistema.
@REM
@REM Uso:
@REM   Sem parâmetros: auditoria padrão (últimas 24 horas)
@REM     03-executar_auditoria.bat
@REM
@REM   Com parâmetros: período específico
@REM     03-executar_auditoria.bat --periodo YYYY-MM-DD YYYY-MM-DD
@REM
@REM Observação:
@REM   Todos os parâmetros informados são repassados ao comando Java
@REM   através de %*.
@REM ================================================================
setlocal
cd /d "%~dp0"
@REM Validação básica: se usar --periodo, exigir duas datas
if /I "%~1"=="--periodo" (
  if "%~2"=="" (
    echo [ERRO] Falta a data inicial. Uso: --periodo YYYY-MM-DD YYYY-MM-DD
    echo [EXEMPLO] 03-executar_auditoria.bat --periodo 2025-10-01 2025-10-31
    goto fim
  )
  if "%~3"=="" (
    echo [ERRO] Falta a data final. Uso: --periodo YYYY-MM-DD YYYY-MM-DD
    echo [EXEMPLO] 03-executar_auditoria.bat --periodo 2025-10-01 2025-10-31
    goto fim
  )
)

@REM Executa auditoria padrão (sem parâmetros) ou com os parâmetros informados
if "%~1"=="" (
  java -jar "target\extrator.jar" --auditoria
) else (
  java -jar "target\extrator.jar" --auditoria %*
)
:fim
pause