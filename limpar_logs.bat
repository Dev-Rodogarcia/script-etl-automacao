@echo off
setlocal EnableExtensions
call "%~dp0scripts\windows\limpar_logs.bat" %*
exit /b %errorlevel%
