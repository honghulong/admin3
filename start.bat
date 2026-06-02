@echo off
title admin3-server

set JAR_FILE=admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar

if not exist "%JAR_FILE%" (
    echo [ERROR] Cannot find JAR file: %JAR_FILE%
    echo Please run "mvn clean package -DskipTests" first
    pause
    exit /b 1
)

echo [INFO] Stopping any existing Java process...
taskkill /f /im java.exe >nul 2>&1
echo [INFO] Waiting for port %SERVER_PORT% to be released...
timeout /t 5 /nobreak >nul

set JAVA_HOME=C:\JAVA\jdk21.0.11-win_x64

set SERVER_PORT=9099

echo [INFO] Using Java: %JAVA_HOME%\bin\java
echo [INFO] Starting: %JAR_FILE%
echo [INFO] URL: http://localhost:%SERVER_PORT%/admin3
echo [INFO] Press Ctrl+C to stop
echo.

"%JAVA_HOME%\bin\java" -jar "%JAR_FILE%" --server.port=%SERVER_PORT% -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8 -Dspring.datasource.username=root -Dspring.datasource.password=123456

echo.
echo [INFO] Service stopped
pause
