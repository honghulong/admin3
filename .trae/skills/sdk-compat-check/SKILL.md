---
name: "sdk-compat-check"
description: "当遇到 SDK 版本兼容性问题（编译错误、API 找不到、运行时异常）时，系统性地去官方文档核实 API 用法 + 去 CSDN/社区搜索用户经验，避免盲目分析源码。"
---

# SDK 版本兼容性核实

当项目中使用的 SDK 版本与代码不兼容（如编译错误、API 签名变化、类/方法找不到、运行时异常）时，使用本技能系统性地查证正确的 API 用法。

## 适用场景

- SDK 升级后代码报错，不确定新版本的 API 变化
- 编译错误提示类/方法/构造函数不存在
- 运行时 NoSuchMethodError、ClassNotFoundException 等
- 不确定某个 API 在特定版本中的正确用法
- 官方文档不清晰，需要参考社区实践经验

## 工作流程

### 第一步：确认 SDK 版本和问题

1. 查看 `pom.xml` / `build.gradle` / `package.json` 等确认当前使用的 SDK 版本
2. 记录具体的错误信息（编译错误、异常堆栈等）
3. 确定需要查证的类名、方法名、API 名称

### 第二步：查阅官方文档

1. **搜索官方文档网站**：使用 `WebSearch` 搜索 `"<sdk-name> <version> documentation"` 或 `"<sdk-name> <version> <api-name>"` 
   - 例如：`"mcp java sdk 0.18.2 documentation"`、`"spring boot 3.2 migration guide"`
2. **直接访问官方文档页面**：使用 `WebFetch` 获取官方文档的具体页面内容
   - 优先查找：API 参考、迁移指南、Release Notes、示例代码
   - 重点关注：类的构造方法、Builder 模式、静态工厂方法等 API 签名
3. **提取关键信息**：从文档中找出正确的 API 用法、类名、方法签名

### 第三步：搜索社区经验

1. **CSDN 搜索**：使用 `WebSearch` 搜索 `"<sdk-name> <version> <api-name> site:csdn.net"`
   - 例如：`"mcp sdk 0.18.2 McpServer site:csdn.net"`
2. **Stack Overflow / GitHub Issues 搜索**：使用 `WebSearch` 搜索 `"<sdk-name> <version> <error-message>"` 或 `"<sdk-name> <api-name> example"`
3. **筛选有效信息**：
   - 优先看发布时间接近的（与 SDK 版本发布时间匹配）
   - 优先看有代码示例的
   - 注意区分不同版本的用法差异

### 第四步：对比验证

1. 将官方文档的 API 签名与社区示例进行对比
2. 如果两者一致，按此方案修改代码
3. 如果存在差异，以官方文档为准，社区经验作为参考
4. 对于不确定的 API，可以使用 `javap` / `jar tf` 等工具反编译确认

### 第五步：修改代码并验证

1. 根据查证结果修改代码
2. 编译验证（`mvn compile` / `npm run build` 等）
3. 运行测试验证功能正常

## 示例

### MCP Java SDK 0.18.2 版本兼容性问题

**问题**：`McpServer.sync(transportProvider)` 返回类型不确定，`addTool` 方法签名变化

**查证过程**：
1. 搜索 `"mcp java sdk 0.18.2 mcp server"` 找到官方文档
2. 从官方文档确认：
   - `McpServer.sync(transport)` 返回 `StatelessSyncSpecification`（无状态模式）
   - 使用 `.tools(spec1, spec2, ...)` 链式调用添加工具
   - 使用 `Tool.builder()` 构建工具定义
   - 使用 `.inputSchema(jsonMapper, jsonString)` 设置输入模式
3. 搜索 CSDN 确认社区使用经验
4. 修改代码并编译验证

### Spring Boot 版本升级问题

**问题**：某个配置类或 API 在新版本中废弃或移除

**查证过程**：
1. 搜索 `"spring boot <old-version> to <new-version> migration guide"`
2. 查看官方迁移指南中的 Breaking Changes
3. 搜索 CSDN 上其他人的升级经验
4. 按文档指引修改代码

## 注意事项

- **官方文档优先**：官方文档是最权威的来源，社区经验仅供参考
- **版本匹配**：确保查证的文档和示例与使用的 SDK 版本匹配
- **反编译验证**：对于关键 API，可以用 `javap` 反编译 jar 包确认签名
- **记录发现**：将查证结果记录在对话中，方便后续参考
- **测试验证**：修改后务必编译和运行测试，确保功能正常
