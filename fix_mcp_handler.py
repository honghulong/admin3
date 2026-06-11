with open(r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java", "r", encoding="utf-8") as f:
    c = f.read()

# Find the handler section
old_start = 'return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {'
old_end = 'SessionItemHolder.getItem("XIAOYI_AGENT_LOGIN_SESSION_ID");'
start_idx = c.find(old_start)
end_idx = c.find(old_end, start_idx) + len(old_end)

old_text = c[start_idx:end_idx]

new_text = '''return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // 优先从工具参数读取，其次从请求头(ThreadLocal)读取
      String sessionId = null;
      if (request.arguments() != null && request.arguments().containsKey("agentLoginSessionId")) {
        sessionId = String.valueOf(request.arguments().get("agentLoginSessionId"));
      }
      if (sessionId == null || sessionId.isBlank() || "null".equals(sessionId)) {
        sessionId = (String) SessionItemHolder.getItem("XIAOYI_AGENT_LOGIN_SESSION_ID");
      }'''

c = c.replace(old_text, new_text)

with open(r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java", "w", encoding="utf-8") as f:
    f.write(c)
print("OK - handler updated")
