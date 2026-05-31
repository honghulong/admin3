import urllib.request
import json
import re

BASE_URL = "http://localhost:8080/admin3/mcp/message"
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
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return {"error": e.code, "body": e.read().decode('utf-8')}
    except Exception as e:
        return {"error": str(e)}

def extract_leave_id(result):
    """从返回结果中提取 leave ID"""
    if "result" in result and "content" in result["result"]:
        for content in result["result"]["content"]:
            if content.get("type") == "text":
                # 匹配 "ID: 110"
                match = re.search(r'ID:\s*(\d+)', content["text"])
                if match:
                    return int(match.group(1))
    return None

# 1. 查询用户 admin 的请假记录
print("=" * 60)
print("1. 查询用户 admin 的请假记录")
print("=" * 60)
result = call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "admin"}
})
print(json.dumps(result, indent=2, ensure_ascii=False))
print()

# 2. 新增请假记录
print("=" * 60)
print("2. 新增请假记录 (admin, 病假)")
print("=" * 60)
result = call_mcp("tools/call", {
    "name": "create_leave",
    "arguments": {
        "username": "admin",
        "leaveType": "sick",
        "startTime": "2026-06-01 09:00:00",
        "endTime": "2026-06-02 18:00:00",
        "leaveReason": "身体不舒服，需要休息"
    }
})
print(json.dumps(result, indent=2, ensure_ascii=False))
print()

leave_id = extract_leave_id(result)
print(f"提取到 leave_id: {leave_id}")
print()

if leave_id:
    # 3. 修改请假记录
    print("=" * 60)
    print(f"3. 修改请假记录 (ID: {leave_id})")
    print("=" * 60)
    result = call_mcp("tools/call", {
        "name": "update_leave",
        "arguments": {
            "leaveId": leave_id,
            "leaveType": "personal",
            "startTime": "2026-06-01 13:00:00",
            "endTime": "2026-06-02 12:00:00",
            "leaveReason": "修改为事假，有私事处理"
        }
    })
    print(json.dumps(result, indent=2, ensure_ascii=False))
    print()

    # 4. 销假
    print("=" * 60)
    print(f"4. 销假 (ID: {leave_id})")
    print("=" * 60)
    result = call_mcp("tools/call", {
        "name": "cancel_leave",
        "arguments": {"leaveId": leave_id}
    })
    print(json.dumps(result, indent=2, ensure_ascii=False))
    print()

# 5. 再次查询确认状态
print("=" * 60)
print("5. 再次查询 admin 的请假记录确认状态")
print("=" * 60)
result = call_mcp("tools/call", {
    "name": "query_leaves_by_username",
    "arguments": {"username": "admin"}
})
print(json.dumps(result, indent=2, ensure_ascii=False))
print()
