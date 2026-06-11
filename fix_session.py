import re

# 1. Fix XiaoYiAuthMcpConfig.java - add agentLoginSessionId as optional param + dual lookup
mcp_file = r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java"
with open(mcp_file, "r", encoding="utf-8") as f:
    c = f.read()

old_tool = """.name("check_binding_status")
      .description("\\u68c0\\u67e5\\u5f53\\u524d\\u5c0f\\u827a\\u4f1a\\u8bdd\\u7684\\u8eab\\u4efd\\u7ed1\\u5b9a\\u72b6\\u6001\\u3002\\u65e0\\u9700\\u53c2\\u6570\\uff0c\\u7cfb\\u7edf\\u81ea\\u52a8\\u4ece\\u8bf7\\u6c42\\u5934\\u4e2d\\u8bfb\\u53d6 agentLoginSessionId\\u3002\\u8fd4\\u56de bound(\\u5df2\\u7ed1\\u5b9a)/unbound(\\u672a\\u7ed1\\u5b9a)/invalid(\\u65e0\\u6548) \\u4e09\\u79cd\\u72b6\\u6001\\uff0c\\u4f9b\\u5de5\\u4f5c\\u6d41\\u51b3\\u7b56")
      .inputSchema(jsonMapper, \"\"\"
        {
          "type": "object",
          "properties": {}
        }
        \"\"\")
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      String sessionId = (String) SessionItemHolder.getItem("XIAOYI_AGENT_LOGIN_SESSION_ID");"""

new_tool = """.name("check_binding_status")
      .description("\\u68c0\\u67e5\\u5f53\\u524d\\u5c0f\\u827a\\u4f1a\\u8bdd\\u7684\\u8eab\\u4efd\\u7ed1\\u5b9a\\u72b6\\u6001\\u3002\\u53ef\\u9009\\u53c2\\u6570 agentLoginSessionId\\uff0c\\u5982\\u4e0d\\u4f20\\u5219\\u4ece\\u8bf7\\u6c42\\u5934\\u4e2d\\u81ea\\u52a8\\u8bfb\\u53d6\\u3002\\u8fd4\\u56de bound(\\u5df2\\u7ed1\\u5b9a)/unbound(\\u672a\\u7ed1\\u5b9a)/invalid(\\u65e0\\u6548) \\u4e09\\u79cd\\u72b6\\u6001\\uff0c\\u4f9b\\u5de5\\u4f5c\\u6d41\\u51b3\\u7b56")
      .inputSchema(jsonMapper, \"\"\"
        {
          "type": "object",
          "properties": {
            "agentLoginSessionId": {
              "type": "string",
              "description": "\\u7528\\u6237\\u6388\\u6743\\u540e\\u7531\\u670d\\u52a1\\u7aef\\u7b7e\\u53d1\\u7684\\u4f1a\\u8bddID\\uff08\\u53ef\\u9009\\uff0c\\u4e0d\\u4f20\\u5219\\u81ea\\u52a8\\u4ece\\u8bf7\\u6c42\\u5934\\u8bfb\\u53d6\\uff09"
            }
          }
        }
        \"\"\")
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // \\u4f18\\u5148\\u4ece\\u5de5\\u5177\\u53c2\\u6570\\u8bfb\\u53d6\\uff0c\\u5176\\u6b21\\u4ece\\u8bf7\\u6c42\\u5934(ThreadLocal) \\u8bfb\\u53d6
      String sessionId = null;
      if (request.arguments() != null && request.arguments().containsKey("agentLoginSessionId")) {
        sessionId = String.valueOf(request.arguments().get("agentLoginSessionId"));
      }
      if (sessionId == null || sessionId.isBlank() || "null".equals(sessionId)) {
        sessionId = (String) SessionItemHolder.getItem("XIAOYI_AGENT_LOGIN_SESSION_ID");
      }"""

if old_tool in c:
    c = c.replace(old_tool, new_tool)
    with open(mcp_file, "w", encoding="utf-8") as f:
        f.write(c)
    print("OK - XiaoYiAuthMcpConfig updated")
else:
    print("FAIL - old tool text not found")

# Verify
with open(mcp_file, "r", encoding="utf-8") as f:
    c = f.read()
print("Has optional param:", "\\u53ef\\u9009" in c or "optional" in c.lower())
print("Has argument lookup:", "request.arguments().containsKey" in c)
print("Has ThreadLocal fallback:", "SessionItemHolder.getItem" in c)
