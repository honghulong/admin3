@echo off
title Log Tail - admin3-server
if "%1"=="" (set "FILTER=") else (set "FILTER=%*")
:loop
cls
echo ========================================
echo  admin3-server 实时日志
echo  过滤: %FILTER%
echo  按 Ctrl+C 退出
echo ========================================
echo.
if "%FILTER%"=="" (
    type "%~dp0logs\admin3-server.log"
) else (
    type "%~dp0logs\admin3-server.log" | findstr "%FILTER%"
)
timeout /t 2 /nobreak >nul
goto loop
