# Generate init_test_data.py
data_script = r"""#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
插入 MCP check_binding_status 测试数据
"""
import mysql.connector
import uuid
from datetime import datetime, timedelta

DB = {"host": "127.0.0.1", "port": 3306, "user": "root", "password": "123456", "database": "admin3", "charset": "utf8mb4"}

cnx = mysql.connector.connect(**DB)
cursor = cnx.cursor()

# 1. 确保测试用户
username = "test_xiaoyi_staff"
cursor.execute("SELECT id, x_employee_id FROM user WHERE username = %s", (username,))
row = cursor.fetchone()
if row:
    user_id, emp_id = row[0], row[1]
    print(f"[user] 已存在: id={user_id}, x_employee_id={emp_id}")
else:
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("INSERT INTO user (username, gender, state, created_time, x_employee_id) VALUES (%s, 0, 0, %s, %s)",
                   (username, now, "EMP-TEST-001"))
    cnx.commit()
    user_id = cursor.lastrowid
    emp_id = "EMP-TEST-001"
    print(f"[user] 新建: id={user_id}, x_employee_id={emp_id}")

# 2. 清理旧的测试数据
cursor.execute("DELETE FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%25'")
cnx.commit()
print("[clean] 已清理旧 test_ 开头的 session")

# 3. 插入 3 条测试 session
now = datetime.now()
sessions = [
    ("test_bound_" + uuid.uuid4().hex[:16],   user_id,  24,  "bound"),
    ("test_unbound_" + uuid.uuid4().hex[:14],  None,     24,  "unbound"),
    ("test_expired_" + uuid.uuid4().hex[:14],  user_id, -1,  "expired"),
]

for sid, uid, hours, label in sessions:
    expire = now + timedelta(hours=hours)
    open_id = f"hw_openid_{sid[-12:]}"
    phone = "13800138000" if uid else None
    sql = """INSERT INTO xiao_yi_user_session
             (agent_login_session_id, phone_number, huawei_open_id, user_id, created_time, expire_time)
             VALUES (%s, %s, %s, %s, %s, %s)"""
    cursor.execute(sql, (sid, phone, open_id, uid, now, expire))
    cnx.commit()
    uid_str = str(uid) if uid else "NULL"
    print(f"[insert] {label:8s}  {sid}  uid={uid_str}  expire={expire.strftime('%m-%d %H:%M')}")

cursor.close()
cnx.close()
print()
print("完成！现在可以运行 python test_check_binding.py 进行测试")
"""

# Generate test_check_binding.py
test_script = r"""#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 XiaoYi MCP check_binding_status 工具

前置条件：
  python init_test_data.py   # 先插入测试数据
  .\start.ps1                # 再启动服务
  python test_check_binding.py  # 运行测试
"""
import sys
import json
import http.client
import time
import mysql.connector

sys.stdout.reconfigure(encoding="utf-8")

BASE_HOST = "localhost"
BASE_PORT = 9099
BASE_URL = f"{BASE_HOST}:{BASE_PORT}"
CONTEXT_PATH = "/admin3"
MCP_AUTH_PATH = f"{CONTEXT_PATH}/xiaoyi-mcp/auth/message"
AUTH_HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026",
}
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "admin3",
    "charset": "utf8mb4",
}


def call_mcp(session_id):
    """调用 check_binding_status"""
    headers = dict(AUTH_HEADERS)
    if session_id:
        headers["agentLoginSessionId"] = session_id

    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": "check_binding_status", "arguments": {}},
    })

    conn = http.client.HTTPConnection(BASE_URL, timeout=10)
    try:
        conn.request("POST", MCP_AUTH_PATH, body, headers)
        resp = conn.getresponse()
        raw_data = resp.read().decode("utf-8")
        return json.loads(raw_data), resp.status
    except Exception as e:
        return {"error": str(e)}, 0
    finally:
        conn.close()


def parse_result(data):
    """从 MCP 响应中提取文本"""
    if "result" in data and "content" in data["result"]:
        for c in data["result"]["content"]:
            if c.get("type") == "text":
                return c["text"], not data["result"].get("isError", False)
    if "error" in data:
        return f"ERROR: {data['error'].get('message', str(data['error']))}", False
    return json.dumps(data, ensure_ascii=False), False


def check_server():
    """检查 MCP 服务"""
    headers = dict(AUTH_HEADERS)
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}})
    conn = http.client.HTTPConnection(BASE_URL, timeout=5)
    try:
        conn.request("POST", MCP_AUTH_PATH, body, headers)
        resp = conn.getresponse()
        if resp.status != 200:
            return False, f"HTTP {resp.status}"
        data = json.loads(resp.read().decode("utf-8"))
        tools = data.get("result", {}).get("tools", [])
        return True, [t["name"] for t in tools]
    except Exception as e:
        return False, str(e)
    finally:
        conn.close()


def run_test(scenario, session_id, expect_status=None):
    """执行单个测试"""
    print()
    print(f"  {'='*56}")
    print(f"  [{scenario}]")
    print(f"  {'='*56}")

    t0 = time.time()
    data, http_code = call_mcp(session_id)
    elapsed = time.time() - t0

    text, is_ok = parse_result(data)
    http_flag = "\u2713" if http_code == 200 else "\u2717"
    sid_display = (session_id[:20] + "...") if session_id and len(session_id) > 24 else (session_id or "null")

    print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  Header: agentLoginSessionId={sid_display}")
    print(f"  返回:")
    for line in text.split("\\n"):
        print(f"    {line}")

    if expect_status:
        if f"\u7ed1\u5b9a\u72b6\u6001: {expect_status}" in text:
            print(f"  \u2500\u2500\u2500\u2500 \u9a8c\u8bc1: \u2713 \u671f\u671b\u72b6\u6001 [{expect_status}] \u5339\u914d")
        else:
            print(f"  \u2500\u2500\u2500\u2500 \u9a8c\u8bc1: \u26a0 \u671f\u671b\u72b6\u6001 [{expect_status}] \u672a\u5339\u914d")
    print()


def main():
    print()
    print("  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557")
    print("  \u2551     XiaoYi MCP  check_binding_status  \u6d4b\u8bd5                              \u2551")
    print("  \u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563")
    print("  \u2551  \u6d4b\u8bd5\u6570\u636e\u5df2\u901a\u8fc7 init_test_data.py \u63d2\u5165                      \u2551")
    print("  \u2551  \u8bf7\u5148\u8fd0\u884c: python init_test_data.py                          \u2551")
    print("  \u2551  \u518d\u8fd0\u884c: python test_check_binding.py                          \u2551")
    print("  \u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563")
    print("  \u2551  sessionId:                                                \u2551")
    print("  \u2551    test_bound_*     \u6709\u6548 + \u5df2\u7ed1\u5b9a                           \u2551")
    print("  \u2551    test_unbound_*   \u6709\u6548 + \u672a\u7ed1\u5b9a                           \u2551")
    print("  \u2551    test_expired_*   \u5df2\u8fc7\u671f                                 \u2551")
    print("  \u2551    (nonexistent)    \u4e0d\u5b58\u5728                               \u2551")
    print("  \u2551    (no header)      \u4e0d\u4f20 agentLoginSessionId                    \u2551")
    print("  \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d")
    print()

    # Step 1: \u68c0\u67e5\u670d\u52a1
    print(f"  [Step 1] \u68c0\u67e5\u670d\u52a1 http://{BASE_URL}{MCP_AUTH_PATH}")
    ok, info = check_server()
    if not ok:
        print(f"  \u2717 \u670d\u52a1\u4e0d\u53ef\u7528: {info}")
        print(f"    \u8bf7\u5148\u542f\u52a8\u670d\u52a1: .\\start.ps1")
        sys.exit(1)
    print(f"  \u2713 \u670d\u52a1\u53ef\u7528\uff0c\u5df2\u6ce8\u518c\u5de5\u5177: {info}")

    # Step 2: \u8bfb\u53d6\u6d4b\u8bd5\u6570\u636e
    print(f"\n  [Step 2] \u8bfb\u53d6\u6d4b\u8bd5\u6570\u636e...")
    try:
        cnx = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("SELECT agent_login_session_id FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%25' ORDER BY created_time")
        rows = cursor.fetchall()
        cursor.close()
        cnx.close()

        sid_map = {}
        for (sid,) in rows:
            if sid.startswith("test_bound_") and "bound" not in sid_map:
                sid_map["bound"] = sid
            elif sid.startswith("test_unbound_") and "unbound" not in sid_map:
                sid_map["unbound"] = sid
            elif sid.startswith("test_expired_") and "expired" not in sid_map:
                sid_map["expired"] = sid

        print(f"  \u2713 \u8bfb\u53d6\u5230 {len(rows)} \u6761 test_ \u8bb0\u5f55")
        if len(sid_map) < 3:
            print(f"  \u26a0 \u7f3a\u5c11\u6d4b\u8bd5\u6570\u636e\uff0c\u8bf7\u5148\u8fd0\u884c: python init_test_data.py")
            sys.exit(1)
    except Exception as e:
        print(f"  \u2717 \u8bfb\u53d6\u6570\u636e\u5931\u8d25: {e}")
        print(f"    \u8bf7\u786e\u8ba4 MySQL \u5df2\u542f\u52a8\u5e76\u8fd0\u884c: python init_test_data.py")
        sys.exit(1)

    # Step 3: \u6267\u884c\u6d4b\u8bd5
    print(f"\n  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557")
    print(f"  \u2551     \u5f00\u59cb\u6d4b\u8bd5\uff08\u5171 5 \u4e2a\u573a\u666f\uff09                              \u2551")
    print(f"  \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d")

    run_test("\u573a\u666f 1: bound   \u2014 \u6709\u6548session + \u5df2\u7ed1\u5b9a",         sid_map["bound"],   "bound")
    run_test("\u573a\u666f 2: unbound \u2014 \u6709\u6548session + \u672a\u7ed1\u5b9a",         sid_map["unbound"], "unbound")
    run_test("\u573a\u666f 3: invalid \u2014 sessionId \u4e0d\u5b58\u5728",                     "nonexistent_session_00001", "invalid")
    run_test("\u573a\u666f 4: expired \u2014 session \u5df2\u8fc7\u671f",                        sid_map["expired"], "invalid")
    run_test("\u573a\u666f 5: no_header \u2014 \u4e0d\u4f20 agentLoginSessionId",               None,               "invalid")

    print(f"\n  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557")
    print(f"  \u2551     \u6240\u6709\u6d4b\u8bd5\u5b8c\u6210                                      \u2551")
    print(f"  \u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d")
    print()


if __name__ == "__main__":
    main()
"""

with open(r"D:\hongmengProjects\admin3\init_test_data.py", "w", encoding="utf-8") as f:
    f.write(data_script)
print("OK - init_test_data.py")

with open(r"D:\hongmengProjects\admin3\test_check_binding.py", "w", encoding="utf-8") as f:
    f.write(test_script)
print("OK - test_check_binding.py")
