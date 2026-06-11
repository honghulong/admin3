with open(r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java", "r", encoding="utf-8") as f:
    c = f.read()

# Fix description
old_d = '.description("\u68c0\u67e5\u5f53\u524d\u5c0f\u827a\u4f1a\u8bdd\u7684\u8eab\u4efd\u7ed1\u5b9a\u72b6\u6001\u3002\u65e0\u9700\u53c2\u6570\uff0c\u7cfb\u7edf\u81ea\u52a8\u4ece\u8bf7\u6c42\u5934\u4e2d\u8bfb\u53d6 agentLoginSessionId\u3002\u8fd4\u56de bound(\u5df2\u7ed1\u5b9a)/unbound(\u672a\u7ed1\u5b9a)/invalid(\u65e0\u6548) \u4e09\u79cd\u72b6\u6001\uff0c\u4f9b\u5de5\u4f5c\u6d41\u51b3\u7b56")'
new_d = '.description("\u68c0\u67e5\u5f53\u524d\u5c0f\u827a\u4f1a\u8bdd\u7684\u8eab\u4efd\u7ed1\u5b9a\u72b6\u6001\u3002\u53ef\u9009\u53c2\u6570 agentLoginSessionId\uff0c\u4e0d\u4f20\u5219\u81ea\u52a8\u4ece\u8bf7\u6c42\u5934\u8bfb\u53d6\u3002\u8fd4\u56de bound(\u5df2\u7ed1\u5b9a)/unbound(\u672a\u7ed1\u5b9a)/invalid(\u65e0\u6548)")'
c = c.replace(old_d, new_d)

# Fix schema
old_s = '"properties": {}'
new_s = '"properties": {\n            "agentLoginSessionId": {\n              "type": "string",\n              "description": "\u4f1a\u8bddID\uff08\u53ef\u9009\uff0c\u4e0d\u4f20\u5219\u81ea\u52a8\u4ece\u8bf7\u6c42\u5934\u8bfb\u53d6\uff09"\n            }\n          }'
c = c.replace(old_s, new_s)

with open(r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java", "w", encoding="utf-8") as f:
    f.write(c)
print("OK")
