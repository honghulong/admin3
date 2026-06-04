---
name: "admin3-project"
description: "admin3 管理系统项目经验记录。包含启动方式、分页配置、前端构建注意事项等。当用户询问 admin3 项目相关问题时自动调用。"
---

# Admin3 项目经验

## 项目结构

- `admin3-ui/` - 前端 Vue 3 + Vite 3 + Element Plus
- `admin3-server/` - 后端 Spring Boot 3.2.3 + Spring Data JPA
- `admin3-parent/` - 父 POM

## 启动方式

### 后端启动（在项目根目录执行）

```powershell
.\mvnw.cmd spring-boot:run -pl admin3-server
```

启动后访问 `http://localhost:9099/admin3`

### 前端构建

```powershell
cd admin3-ui
node node_modules/vite/bin/vite.js build
```

> **注意**：Trae 沙箱中 `npm`/`npx`/`yarn` 可能受 PowerShell 执行策略限制无法直接执行，使用 `node node_modules/vite/bin/vite.js build` 替代。

### 全量打包（含前端资源复制到 JAR）

```powershell
.\mvnw.cmd package -pl admin3-server -am -DskipTests
```

> **注意**：`pom.xml` 中 frontend-maven-plugin 的 `yarn build` 步骤被注释掉了，所以 Maven 打包时不会自动构建前端。需要先手动 `vite build` 再 `mvn package`。

## 分页配置

### 关键配置

`application.yml` 中配置了：

```yaml
spring:
  data:
    web:
      pageable:
        one-indexed-parameters: true
```

这意味着 Spring Data 的 `Pageable` 接受 page 参数从 **1** 开始（而不是默认的 0）。

### 分页传参规范

- 前端传 `page: query.pageIndex`（第 1 页传 1，第 2 页传 2）
- 后端 Controller 直接用 `Pageable pageable`，**不需要**手动 `page - 1`
- 后端返回格式：`PageDTO<T>(List<T> list, long total)`，序列化为 `{list: [...], total: N}`

### 踩坑记录

**问题**：报销管理分页不生效，第 1 页和第 2 页返回相同数据。
**原因**：前端传了 `page: query.pageIndex - 1`（第 1 页传 0），但 `one-indexed-parameters: true` 配置下，page=0 被视为无效值，Spring Data 默认返回第 1 页。
**修复**：将 `page: query.pageIndex - 1` 改为 `page: query.pageIndex`。

## 前端构建注意事项（Trae 沙箱环境）

1. **日志输出可能被截断**：`vite build` 的输出可能只显示 "transforming..." 就卡住，但实际上构建可能已完成。判断方法：检查 `dist/assets/` 目录的文件时间戳是否更新。
2. **不要过早中断**：等待至少 2-3 分钟再判断是否卡住，或者检查 dist 目录文件变化。
3. **PowerShell 执行策略限制**：`npm`、`npx`、`yarn` 命令可能无法直接执行，使用 `node node_modules/<package>/bin/<script>.js` 方式调用。

## Playwright 浏览器安装

Trae 内置的 Playwright MCP 工具需要 `chromium-1200` 版本。如果通过 `playwright-core` 安装了不同版本（如 `chromium-1223`），可以创建目录符号链接来解决：

```powershell
New-Item -ItemType Junction -Path "C:\Users\Administrator\AppData\Local\ms-playwright\chromium-1200" -Target "C:\Users\Administrator\AppData\Local\ms-playwright\chromium-1223"
```

## 后端分页查询 JPQL 写法

使用 `LIKE` 查询时，正确的写法是：

```java
@Query("... WHERE (:keyword IS NULL OR r.title LIKE CONCAT('%', :keyword, '%'))")
```

不要使用 `LIKE %:keyword%` 这种写法，Spring Data JPA 不支持。

## 小艺智能体 MCP 服务

### 架构说明

项目中有两套 MCP 服务：

1. **请假 MCP** (`XiaoYiMcpConfig.java`) - endpoint: `/xiaoyi-mcp/message`
2. **报销 MCP** (`ReimbursementMcpConfig.java`) - endpoint: `/xiaoyi-mcp/reimbursement/message`

### 认证方式

所有 `/xiaoyi-mcp/` 路径的请求都通过 `XiaoYiAuthFilter` 进行 Header 鉴权：

- Header: `appId` + `apiKey`
- 默认值在 `application.yml` 中配置：
  ```yaml
  xiaoyi:
    mcp:
      auth:
        appId: admin3-leave-app
        apiKey: admin3-leave-api-key-2026
  ```
- 认证配置类: `XiaoYiAuthProperties.java`
- 传输方式: Streamable HTTP（无状态），基于 `WebMvcStatelessServerTransport`

### 报销 MCP 提供的 Tool

1. **`ocr_upload_reimbursement`** - 上传发票图片进行 OCR 识别，创建草稿报销单，返回识别字段供用户确认
   - 参数: `imageBase64`(图片Base64), `filename`(文件名)
   - 内部调用本地 OCR MCP 服务 (`http://localhost:8765`) 的 `ocr_invoice_base64` 工具
   - 自动从 OCR 结果中提取发票号码、金额、日期等字段
   - 创建状态为 `draft` 的草稿报销单

2. **`confirm_reimbursement`** - 用户确认后，将草稿报销单提交为正式报销单（进入审批流程）
   - 参数: `reimbursementId`(草稿ID), `category`(报销分类), `title`(可选), `amount`(可选)
   - 报销分类: travel(差旅), office(办公), entertainment(招待), other(其他)

3. **`query_reimbursements_by_username`** - 按用户姓名查询个人报销单列表
   - 参数: `username`(用户名)
   - 返回报销单的详细信息（ID、标题、分类、金额、状态等）

### 本地 OCR 服务

- 服务地址: `http://localhost:8765`
- 基于 PaddleOCR 的发票识别服务
- 提供 3 个 Tool: `ocr_invoice`, `ocr_invoice_base64`, `ocr_health`
- 启动方式: 在 `D:\hongmengProjects\ocr-invoice-mcp\` 目录下运行 `uv run python ocr_server.py`
- Java 通过 HttpClient 调用 OCR 服务的 MCP 协议接口

### 添加新 MCP 服务的步骤

1. 创建新的 `*McpConfig.java` 配置类（放在 `infra/mcp/xiaoyi/` 包下）
2. 定义不同的 `messageEndpoint`（如 `/xiaoyi-mcp/xxx/message`）
3. 认证自动由 `XiaoYiAuthFilter` 处理（拦截所有 `/xiaoyi-mcp/` 路径）
4. 在 `application.yml` 中配置认证信息（可复用或新增）
5. 确保 `WebMvcConfiguration` 中 `/xiaoyi-mcp/**` 路径被排除在登录拦截器之外
