with open(r"D:\hongmengProjects\admin3\test_check_binding.py", "r", encoding="utf-8") as f:
    c = f.read()

old = """    headers = dict(AUTH_HEADERS)
    if session_id:
        headers["agentLoginSessionId"] = session_id

    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": "check_binding_status", "arguments": {}},
    })"""

new = """    headers = dict(AUTH_HEADERS)

    args = {}
    if session_id:
        args["agentLoginSessionId"] = session_id

    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": "check_binding_status", "arguments": args},
    })"""

c = c.replace(old, new)
with open(r"D:\hongmengProjects\admin3\test_check_binding.py", "w", encoding="utf-8") as f:
    f.write(c)
print("OK")
