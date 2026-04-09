@echo off
setlocal EnableExtensions
call "%~dp0scripts\windows\00-PRODUCAO_START.bat" %*
exit /b %errorlevel%
