import urllib.request
import json
import re

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


print_result("1. tools/list", call_mcp("tools/list"))

print_result("2. query_leaves_by_username (admin)", call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "admin"}
}))

result = call_mcp("tools/call", {
    "name": "create_leave",
    "arguments": {
        "username": "admin",
        "leaveType": "sick",
        "startTime": "2026-06-02 09:00:00",
        "endTime": "2026-06-03 18:00:00",
        "leaveReason": "身体不舒服，需要休息"
    }
})
print_result("3. create_leave", result)

leave_id = extract_leave_id(result)
print(f"\n>>> leave_id: {leave_id}")

if leave_id:
    print_result(f"4. update_leave (ID={leave_id})", call_mcp("tools/call", {
        "name": "update_leave",
        "arguments": {
            "leaveId": leave_id,
            "leaveType": "personal",
            "startTime": "2026-06-02 13:00:00",
            "endTime": "2026-06-03 12:00:00",
            "leaveReason": "修改为事假"
        }
    }))

    print_result(f"5. cancel_leave (ID={leave_id})", call_mcp("tools/call", {
        "name": "cancel_leave",
        "arguments": {"leaveId": leave_id}
    }))

print_result("6. 再次查询确认", call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "admin"}
}))
