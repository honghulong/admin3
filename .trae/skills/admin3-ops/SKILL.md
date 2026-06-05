---
name: "admin3-ops"
description: "admin3 报销 MCP 服务测试运维经验。涉及启动服务、运行测试脚本、PowerShell 避坑指南。当用户要求启动/测试/检查 admin3 报销 MCP 服务时自动调用。"
---

# admin3 报销 MCP 服务测试运维指南

## 1. 服务启动

### 启动脚本
使用 `start.bat` 启动服务（位于项目根目录 `d:\hongmengProjects\admin3\`）。

```powershell
Set-Location D:\hongmengProjects\admin3; .\start.bat
```

**核心原则：服务一旦启动就不要关，除非代码有变更需要重新编译。**
- `start.bat` 使用 `start /B` 后台启动 Java 进程，不依赖终端窗口
- 启动后终端立即返回，可以继续执行其他命令
- 服务进程独立运行，关闭终端不会导致服务退出
- 重复执行 `start.bat` 会自动检测已有实例，不会重复启动

**启动后需要等待约 10-15 秒服务才完全就绪。**

### 停止服务
```powershell
Set-Location D:\hongmengProjects\admin3; .\stop.bat
```

### 检查服务状态
```powershell
try { $r = Invoke-WebRequest -Uri "http://localhost:9099/admin3/actuator/health" -UseBasicParsing -TimeoutSec 5; Write-Host "UP: $($r.StatusCode)" } catch { Write-Host "DOWN" }
```

**注意：**
- PowerShell 的 `curl` 是 `Invoke-WebRequest` 别名，不是真正的 curl，不支持同样的参数
- 必须加 `-UseBasicParsing` 参数避免解析 HTML 报错
- 不要用 `curl -s` 这种 Linux curl 语法

### 查看启动进度
```powershell
Get-Content D:\hongmengProjects\admin3\logs\admin3-server.log -Tail 5
```
看到 `Tomcat started on port 9099` 和 `Started Admin3ServerApplication` 即表示启动完成。

## 2. 运行测试脚本

### 测试脚本位置
`D:\hongmengProjects\admin3\test_reim_mcp_quick.py`

### 运行方式
```powershell
Set-Location D:\hongmengProjects\admin3; python test_reim_mcp_quick.py
```

### 输出编码问题
Python 脚本中已设置 `sys.stdout.reconfigure(encoding='utf-8')`，但 PowerShell 终端显示中文可能乱码（UTF-8 被当成 GBK 显示）。

**可靠方案：将输出重定向到文件后用 UTF-8 读取**
```powershell
Set-Location D:\hongmengProjects\admin3; python test_reim_mcp_quick.py > test_output.log 2>&1
Get-Content test_output.log -Encoding UTF8
```

### 前置依赖
- 需要安装 `mysql-connector-python`：`pip install mysql-connector-python`
- 同级目录下需要 `test-invoice.png` 测试图片
- 服务必须已启动（`http://localhost:9099/admin3`）

### 数据库配置（脚本中硬编码）
```python
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'admin3',
}
```

## 3. 常见问题排查

### 服务启动失败
1. 检查 JAR 包是否存在：`admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar`
2. 如果不存在，先 `mvn clean package -DskipTests`
3. 查看 `logs/admin3-server.log` 中的错误堆栈

### 测试脚本执行报错
1. `ModuleNotFoundError: No module named 'mysql.connector'` → 安装 `mysql-connector-python`
2. `FileNotFoundError: test-invoice.png` → 检查图片文件是否存在
3. 连接数据库失败 → 检查 MySQL 是否在 127.0.0.1:3306 运行，用户名密码是否正确

### PowerShell 注意事项
| 操作 | 不要用 | 要用 |
|------|--------|------|
| 命令连接 | `&&` | `;` |
| HTTP 请求 | `curl -s` | `Invoke-WebRequest -UseBasicParsing` |
| 等待 | `timeout /t 10 >nul` | `Start-Sleep -Seconds 10` |
| 查看文件 | `cat file` | `Get-Content file -Encoding UTF8` |

## 4. 服务生命周期管理

- **服务后台运行，不依赖终端**：`start.bat` 使用 `start /B` 后台启动，关闭终端不影响服务
- **启动一次，反复测试**：只要代码没变、没手动 `stop.bat`，服务就一直可用
- **重复启动安全**：多次执行 `start.bat` 会自动检测已有实例，不会重复启动
- **测试前检查服务状态**：用 `Invoke-WebRequest` 确认服务在运行
- **需要重新编译时才停服务**：`mvn clean package` 前先 `stop.bat`，编译完再 `start.bat`
