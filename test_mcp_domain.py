import urllib.request
import json
import re

# 改成你的域名
BASE_URL = "https://28wh9253sf76.vicp.fun/admin3/mcp/message"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream"
}

def call_mcp(method, params=None):
    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params or {}
    }).encode('utf-8')
    req = urllib.request.Request(BASE_URL, data=body, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return {"error": e.code, "body": e.read().decode('utf-8')}
    except Exception as e:
        return {"error": str(e)}

# 查询用户 admin 的请假记录
print("=" * 60)
print("查询用户 admin 的请假记录")
print("=" * 60)
result = call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "admin"}
})
print(json.dumps(result, indent=2, ensure_ascii=False))
