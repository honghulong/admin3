with open(r"D:\hongmengProjects\admin3\admin3-server\src\main\java\tech\wetech\admin3\infra\mcp\xiaoyi\XiaoYiAuthMcpConfig.java", "r", encoding="utf-8") as f:
    c = f.read()
idx = c.find(".name(\"check_binding_status\")")
if idx >= 0:
    print(c[idx:idx+800])
else:
    print("NOT FOUND")
