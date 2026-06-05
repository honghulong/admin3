---
name: "build-and-run"
description: "编译和启动项目。编译后只提供启动脚本让用户自己看日志，除非能直接启动到 Trae 终端控制台窗口中。"
---

# 编译和启动项目

## 编译项目

### 命令

```bash
mvn clean package -DskipTests
```

**注意**：`-Dmaven.test.skip=true` 不要和 `-DskipTests` 一起用，PowerShell 下 `-Dmaven.test.skip=true` 会被解析成两个参数导致 `Unknown lifecycle phase` 错误。只用 `-DskipTests` 即可。

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

### 编译流程与等待策略

完整编译流程：

1. **停旧进程** → `taskkill /f /im java.exe`
2. **mvn clean package** → 自动执行前端构建 + 后端编译
3. **验证产物** → 检查 JAR 是否存在

#### transforming... 阶段（关键！）

`mvn clean package` 执行到前端构建时，日志会停在：

```
[INFO] vite v3.2.7 building for production...
[INFO] transforming...
```

**`transforming...` 从来没有卡死过。** 这是 Vite 用 esbuild 并行编译所有 `.ts`/`.vue` 文件的阶段，这个阶段**没有进度条**，也不输出任何中间日志，直到全部编译完成才会一次性输出结果。

**正确做法：**
1. 看到 `transforming...` 后，**直接等 20-30 秒**
2. 用 `Get-Process -Name "node"` 检查 node 进程是否还在运行
   - 如果 node 进程 CPU 和内存持续增长 → 正在编译，继续等
   - 如果 node 进程已退出 → 编译完成
3. 检查 `admin3-ui/dist` 目录是否有文件（69 个左右）
4. 检查 `admin3-server/target/*.jar` 是否生成

**不需要提前焦虑，不需要反复检查 `CheckCommandStatus`。** 直接等 20 秒后检查产物即可。

#### 各阶段耗时参考

| 阶段 | 耗时 | 说明 |
|------|------|------|
| `yarn install` | ~0.3s | 依赖已存在时极快 |
| `transforming...` | **15-25s** | 最耗时的阶段，无进度输出 |
| 后端编译打包 | ~7s | 正常 Java 编译速度 |
| **总计** | **~25s** | 完整构建 |

#### 验证构建结果

```powershell
# 检查 JAR 是否存在
Get-ChildItem admin3-server\target\*.jar -Name

# 检查前端 dist
Get-ChildItem admin3-ui\dist -Recurse -Name | Measure-Object -Line

# 检查 JAR 中是否包含前端资源
jar tf admin3-server/target/admin3-server-0.0.1-SNAPSHOT.jar | Select-String "index.html"
```

## 启动项目

### 方式一：后台启动（推荐）

使用 `start.bat` 后台启动，不依赖终端窗口，关掉终端服务也不会停。

```batch
.\start.bat
```

启动脚本特点：
- **后台启动**：用 `start /B` 启动 Java 进程，终端关闭不影响服务
- **自动检测**：重复执行会自动检测已有实例，不会重复启动
- **日志输出**：标准输出和错误输出都重定向到 `logs/admin3-server.log`
- **PID 记录**：保存 PID 到 `logs/admin3-server.pid`

停止服务使用 `stop.bat`：

```batch
.\stop.bat
```

### 方式二：直接启动到 Trae 终端控制台

如果用户要求直接启动到 Trae 终端控制台中查看日志，可以使用以下命令：

```bash
"%JAVA_HOME%\bin\java" -jar admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8 -Dspring.datasource.username=root -Dspring.datasource.password=123456
```

### 服务生命周期管理

**核心原则：服务启动一次就一直用着，除非代码改了需要重新编译，否则不用关。**

- 启动后服务在后台持续运行，不依赖任何终端窗口
- 下次测试直接跑脚本就行，不用重新启动服务
- 只有以下情况需要重启：
  - 代码修改后重新编译（`mvn clean package`）
  - 服务异常退出
  - 用户明确要求停止

### 检查服务状态

```powershell
# 检查 HTTP 响应
Invoke-WebRequest -Uri "http://localhost:9099/admin3/" -UseBasicParsing | Select-Object StatusCode

# 检查 Java 进程
Get-Process -Name "java" -ErrorAction SilentlyContinue | Select-Object Id, CPU, WorkingSet64

# 查看日志
Get-Content -Path "logs/admin3-server.log" -Encoding UTF8 -Tail 20
```

**注意**：必须用 `Invoke-WebRequest -UseBasicParsing`，不能用 `curl -s`（PowerShell 的 curl 是 `Invoke-WebRequest` 别名，参数不兼容）。

## 注意事项

- 项目需要 Java 21+ 运行环境
- 需要先启动 MySQL 数据库，并创建 `admin3` 数据库
- 默认访问地址：`http://localhost:9099/admin3`
- 默认数据库配置：`jdbc:mysql://127.0.0.1:3306/admin3`，用户名 `root`，密码 `123456`
- 编译前必须先停掉正在运行的 Java 进程，否则 JAR 文件被锁定无法覆盖

## 前端静态资源 404 排查清单

### 背景

项目结构：`admin3-ui`（前端模块）→ 打包为 JAR → `admin3-server` 依赖它 → Spring Boot 提供服务。

前端资源路径链路：
- `vite.config.ts` 中 `base` 决定 `index.html` 中引用的 JS/CSS 路径
- `admin3-ui/pom.xml` 中 `resources` 决定前端构建产物打包到 JAR 的哪个路径
- `admin3-server` 的 `WebMvcConfiguration.java` 中 `addResourceHandlers` 决定静态资源 URL 到实际路径的映射

### 排查步骤（按顺序）

1. **先确认 JAR 里有没有资源**
   ```bash
   jar tf admin3-server/target/admin3-server-0.0.1-SNAPSHOT.jar | Select-String "index.html"
   ```
   - 如果没有 → 检查 `admin3-ui/pom.xml` 的 `resources` 配置是否正确
   - 如果有 → 继续下一步

2. **确认资源在 JAR 中的实际路径**
   ```bash
   jar tf admin3-server/target/admin3-server-0.0.1-SNAPSHOT.jar | Select-String "el-card"
   ```
   记录实际路径，例如 `META-INF/resources/webjars/admin3-ui/assets/el-card.xxx.js`

3. **确认 `index.html` 中引用的路径**
   ```bash
   # 启动服务后
   Invoke-WebRequest -Uri "http://localhost:9099/admin3/" -UseBasicParsing | Select-Object -ExpandProperty Content
   ```
   查看 `<script>` 和 `<link>` 标签中的 `src`/`href` 路径

4. **确认 `WebMvcConfiguration.java` 中的资源映射**
   查看 `addResourceHandlers` 方法，确保 URL 路径能映射到 JAR 中的实际路径

5. **确认拦截器排除路径**
   查看 `addInterceptors` 中的 `excludePathPatterns`，确保静态资源路径不被拦截

### 关键配置对应关系

| 配置位置 | 作用 | 示例值 |
|---------|------|--------|
| `vite.config.ts` → `base` | 决定 `index.html` 中资源引用路径 | `'/admin3/webjars/admin3-ui/'` |
| `admin3-ui/pom.xml` → `resources.targetPath` | 决定构建产物在 JAR 中的路径 | `META-INF/resources/webjars/admin3-ui` |
| `WebMvcConfiguration.java` → `addResourceHandlers` | 决定 URL 到 JAR 路径的映射 | `/**` → `classpath:/META-INF/resources/webjars/admin3-ui/` |

### 核心原则

**不要同时改多个配置。** 一次只改一个，验证通过后再改下一个。否则出了问题不知道是哪个改坏的。
