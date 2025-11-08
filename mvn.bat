@echo off
REM ================================================================
REM Wrapper para Maven que configura JAVA_HOME automaticamente
REM Uso: mvn clean package (funciona como o Maven normal)
REM ================================================================

REM Configurar JAVA_HOME
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Chamar o Maven real com todos os argumentos
C:\apache-maven-3.9.10\bin\mvn.cmd %*
