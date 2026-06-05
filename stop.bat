@echo off
title admin3-server-stop

set LOG_DIR=%~dp0logs
set PID_FILE=%LOG_DIR%\admin3-server.pid

if not exist "%PID_FILE%" (
    echo [INFO] No PID file found. Trying to kill all java.exe...
    taskkill /f /im java.exe >nul 2>&1
    echo [INFO] Done.
    goto :EOF
)

set /p PID=<"%PID_FILE%"
echo [INFO] Stopping service (PID: %PID%)...

taskkill /f /pid %PID% >nul 2>&1
if errorlevel 1 (
    echo [WARN] Process %PID% not found, may have already exited.
) else (
    echo [INFO] Service stopped.
)

del "%PID_FILE%" 2>nul
echo.
