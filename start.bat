@echo off
title admin3-server

set JAR_FILE=admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar
set LOG_DIR=%~dp0logs
set LOG_FILE=%LOG_DIR%\admin3-server.log
set PID_FILE=%LOG_DIR%\admin3-server.pid

if not exist "%JAR_FILE%" (
    echo [ERROR] Cannot find JAR file: %JAR_FILE%
    echo Please run "mvn clean package -DskipTests" first
    pause
    exit /b 1
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /fi "PID eq %OLD_PID%" 2>nul | findstr "%OLD_PID%" >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] Service already running (PID: %OLD_PID%)
        echo [INFO] Log file: %LOG_FILE%
        echo [INFO] URL: http://localhost:9099/admin3
        goto :EOF
    )
)

echo [INFO] Starting: %JAR_FILE%
echo [INFO] URL: http://localhost:9099/admin3
echo [INFO] Log file: %LOG_FILE%
echo.

start /B "" "%JAVA_HOME%\bin\java" -jar "%JAR_FILE%" ^
    -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8 ^
    -Dspring.datasource.username=root ^
    -Dspring.datasource.password=123456 ^
    >> "%LOG_FILE%" 2>&1

set "PID="
for /f "tokens=2 delims=," %%a in ('wmic process where "name='java.exe' and commandline like '%%admin3-server%%'" get processid /format:csv 2^>nul') do set "PID=%%a"
if defined PID (
    echo %PID%>"%PID_FILE%"
    echo [INFO] Service started (PID: %PID%)
) else (
    echo [WARN] Could not detect PID, but service should be starting...
)

echo [INFO] Use stop.bat to stop the service
echo.
