---
name: "build-and-run"
description: "编译和启动项目。编译后只提供启动脚本让用户自己看日志，除非能直接启动到 Trae 终端控制台窗口中。"
---

# 编译和启动项目

## 编译项目

使用 Maven 编译打包（跳过测试）：

```bash
mvn clean package -DskipTests -Dmaven.test.skip=true
```

编译产物在 `admin3-server/target/admin3-server-0.0.1-SNAPSHOT.jar`

### 编译前注意：先停掉旧进程

如果 Java 进程正在运行（通过 `java -jar` 启动了应用），JAR 文件会被操作系统**锁定**，此时执行 `mvn clean package` 会编译失败，报错：

```
[ERROR] Failed to execute goal ...: The specified file is locked or in use by another process
```

**原因**：`mvn clean` 会删除 `target` 目录下的旧 JAR 包，但正在运行的 Java 进程持有该文件的句柄，Windows 不允许删除/覆盖被占用的文件。

**解决办法**：编译前先停掉正在运行的 Java 进程：

```bash
# Windows
taskkill /f /im java.exe

# Linux/Mac
kill -9 $(lsof -t -i:8080)
```

或者在启动脚本中自动处理（见下方 `start.bat` 示例）。

## 启动项目

### 方式一：提供启动脚本（推荐）

编译完成后，创建 `start.bat` 启动脚本给用户，让用户自己双击运行查看日志。

启动脚本应包含**自动停掉旧进程**的逻辑：

```batch
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
timeout /t 2 /nobreak >nul

echo [INFO] Using Java: %JAVA_HOME%\bin\java
echo [INFO] Starting: %JAR_FILE%
echo [INFO] URL: http://localhost:8080/admin3
echo [INFO] Press Ctrl+C to stop
echo.

"%JAVA_HOME%\bin\java" -jar "%JAR_FILE%" -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8 -Dspring.datasource.username=root -Dspring.datasource.password=123456

echo.
echo [INFO] Service stopped
pause
```

### 方式二：直接启动到 Trae 终端控制台

如果用户要求直接启动到 Trae 终端控制台中查看日志，可以使用以下命令：

```bash
"%JAVA_HOME%\bin\java" -jar admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8 -Dspring.datasource.username=root -Dspring.datasource.password=123456
```

## 注意事项

- 项目需要 Java 21+ 运行环境
- 需要先启动 MySQL 数据库，并创建 `admin3` 数据库
- 默认访问地址：`http://localhost:8080/admin3`
- 默认数据库配置：`jdbc:mysql://127.0.0.1:3306/admin3`，用户名 `root`，密码 `123456`
- 编译前必须先停掉正在运行的 Java 进程，否则 JAR 文件被锁定无法覆盖
