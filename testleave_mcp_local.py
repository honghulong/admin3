import urllib.request
import json
import re
# 测试请假 域名MCP 接口
BASE_URL = "https://28wh9253sf76.vicp.fun/admin3/xiaoyi-mcp/message"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026"
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
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return {"error": e.code, "body": e.read().decode('utf-8')}
    except Exception as e:
        return {"error": str(e)}


def extract_leave_id(result):
    if "result" in result and "content" in result["result"]:
        for content in result["result"]["content"]:
            if content.get("type") == "text":
                match = re.search(r'ID:\s*(\d+)', content["text"])
                if match:
                    return int(match.group(1))
    return None


def print_result(label, result):
    print(f"\n{'=' * 60}")
    print(f"  {label}")
    print(f"{'=' * 60}")
    print(json.dumps(result, indent=2, ensure_ascii=False))


print_result("查询刘小波的请假信息", call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "刘小波"}
}))
