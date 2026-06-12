---
name: "admin3-mcp-dev"
description: "admin3 项目 MCP 服务开发经验记录。包含常见错误、踩坑记录和最佳实践。当用户询问 admin3 MCP 开发相关问题或创建新 MCP 服务时自动调用。"
---

# admin3 MCP 服务开发经验

## 1. 关键踩坑记录

### 1.1 Accept Header 必须包含 `application/json, text/event-stream`

**问题**: MCP SDK 0.18.2 的 `WebMvcStatelessServerTransport.handlePost()` 方法会检查请求的 `Accept` 头：
1. 必须包含 `application/json`
2. 必须同时包含 `text/event-stream`

如果缺少任何一个，直接返回 HTTP 400 Bad Request。

**解决方案**: 所有 MCP 请求的 HTTP Header 中必须设置：
```
Accept: application/json, text/event-stream
```

### 1.2 ThreadLocal 跨线程问题

**问题**: `XiaoYiAuthFilter` 在 Tomcat 工作线程（如 `nio-9099-exec-1`）中执行，将 `agentLoginSessionId` 存入 `ThreadLocal`。但 MCP SDK 内部使用 Reactor 异步处理请求，handler 在 Reactor 线程池（如 `oundedElastic-1`）中执行。不同线程无法读取 `ThreadLocal` 中的值。

**解决方案**: 使用 MCP SDK 提供的 `contextExtractor` 机制：
1. Filter 中将 `agentLoginSessionId` 设置到 `request.setAttribute()` 中
2. 在 `WebMvcStatelessServerTransport.Builder` 中配置 `contextExtractor`，从 `ServerRequest.servletRequest()` 的属性中提取
3. 通过 `McpTransportContext.create(Map.of("KEY", value))` 创建上下文
4. 在工具 handler 中通过 `McpTransportContext.get("KEY")` 获取

**代码示例**:
```java
// 1. Filter 中设置属性
request.setAttribute("XIAOYI_AGENT_LOGIN_SESSION_ID", agentLoginSessionId);

// 2. Transport 配置 contextExtractor
WebMvcStatelessServerTransport.builder()
    .contextExtractor(serverRequest -> {
        HttpServletRequest servletRequest = (HttpServletRequest) serverRequest.servletRequest();
        String sessionId = (String) servletRequest.getAttribute("XIAOYI_AGENT_LOGIN_SESSION_ID");
        if (sessionId != null) {
            return McpTransportContext.create(Map.of("XIAOYI_AGENT_LOGIN_SESSION_ID", sessionId));
        }
        return McpTransportContext.EMPTY;
    })
    .build();

// 3. Handler 中获取（注意：McpStatelessServerFeatures.SyncToolSpecification
//    的构造函数是 BiFunction<McpTransportContext, CallToolRequest, CallToolResult>）
return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
    String sessionId = (String) ctx.get("XIAOYI_AGENT_LOGIN_SESSION_ID");
    // ...
});
```

### 1.3 多个 RouterFunction Bean 冲突

**问题**: 每个 MCP 服务都会注册一个 `RouterFunction<ServerResponse>` bean。如果有多个 MCP 服务（如 binding、reimbursement、leave），Spring 会合并所有 `RouterFunction` bean。如果路径不同，不会冲突。

**注意**: 如果请求返回 400，优先检查 Accept Header，而不是怀疑 RouterFunction 冲突。

## 2. MCP 服务开发步骤

1. 创建 `XxxMcpConfig.java` 配置类
2. 定义 `McpJsonMapper` bean
3. 定义 `WebMvcStatelessServerTransport` bean（配置 `contextExtractor`）
4. 定义 `RouterFunction<ServerResponse>` bean（调用 `transport.getRouterFunction()`）
5. 定义 `McpStatelessSyncServer` bean（注册 tools）
6. 创建工具方法，使用 `McpStatelessServerFeatures.SyncToolSpecification`

## 3. 测试注意事项

- 所有 MCP 请求必须带 `Accept: application/json, text/event-stream` header
- `agentLoginSessionId` 放在 HTTP Header 中传递
- 使用 `McpTransportContext` 跨线程传递数据，不要依赖 `ThreadLocal`

## 4. 相关文件

- MCP 配置: `admin3-server/src/main/java/tech/wetech/admin3/infra/mcp/xiaoyi/XiaoYiBindingMcpConfig.java`
- Auth Filter: `admin3-server/src/main/java/tech/wetech/admin3/infra/mcp/xiaoyi/XiaoYiAuthFilter.java`
- 测试脚本: `test_check_binding.py`
