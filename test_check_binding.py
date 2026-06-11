#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 XiaoYi MCP check_binding_status 工具

前置条件:
  1) python init_userbindtest_data.py    # 先插入测试数据
  2) .\start.ps1                 # 再启动服务
  3) python test_check_binding.py

5 个测试场景:
  bound    - 有效 session + 已绑定员工
  unbound  - 有效 session + 未绑定员工
  invalid  - sessionId 不存在
  expired  - session 已过期
  no_header - 不传 agentLoginSessionId header
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
    """调用 check_binding_status MCP 工具"""
    headers = dict(AUTH_HEADERS)

    args = {}
    if session_id:
        args["agentLoginSessionId"] = session_id

    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": "check_binding_status", "arguments": args},
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
    """从 MCP JSON-RPC 响应中提取文本"""
    if "result" in data and "content" in data["result"]:
        for c in data["result"]["content"]:
            if c.get("type") == "text":
                is_error = data["result"].get("isError", False)
                return c["text"], not is_error
    if "error" in data:
        return f"ERROR: {data['error']}", False
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
    """执行单个测试用例"""
    print()
    print(f"  {'='*56}")
    print(f"  [{scenario}]")
    print(f"  {'='*56}")

    t0 = time.time()
    data, http_code = call_mcp(session_id)
    elapsed = time.time() - t0

    text, is_ok = parse_result(data)
    http_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
    sid_display = (session_id[:20] + "...") if session_id and len(session_id) > 24 else (session_id or "null")

    print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  Header: agentLoginSessionId={sid_display}")
    print(f"  返回:")
    for line in text.split(chr(0x5c)+"n"):
        print(f"    {line}")

    if expect_status:
        expected = chr(0x7ed1)+chr(0x5b9a)+chr(0x72b6)+chr(0x6001)+chr(0x3a)+" "+expect_status
        if expected in text:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x2713)} 期望状态 [{expect_status}] 匹配")
        else:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x26a0)} 期望状态 [{expect_status}] 未匹配")
    print()


def main():
    print()
    print("  "+chr(0x2554)+chr(0x2550)*56+chr(0x2557))
    print(f"  {chr(0x2551)}     XiaoYi MCP  check_binding_status  测试                    {chr(0x2551)}")
    print("  "+chr(0x2560)+chr(0x2550)*56+chr(0x2563))
    print(f"  {chr(0x2551)}  数据已通过 init_userbindtest_data.py 插入                          {chr(0x2551)}")
    print(f"  {chr(0x2551)}  请先运行: python init_userbindtest_data.py                          {chr(0x2551)}")
    print(f"  {chr(0x2551)}  再运行: python test_check_binding.py                          {chr(0x2551)}")
    print("  "+chr(0x2560)+chr(0x2550)*56+chr(0x2563))
    print(f"  {chr(0x2551)}  sessionId 前缀:                                           {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_bound_*     有效 + 已绑定                           {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_unbound_*   有效 + 未绑定                           {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_expired_*   已过期                                  {chr(0x2551)}")
    print("  "+chr(0x255a)+chr(0x2550)*56+chr(0x255d))

    # Step 1: 检查服务
    print(f"\n  [Step 1] 检查服务 http://{BASE_URL}{MCP_AUTH_PATH}")
    ok, info = check_server()
    if not ok:
        print(f"  {chr(0x2717)} 服务不可用: {info}")
        print(f"    请先启动服务: .\\start.ps1")
        sys.exit(1)
    print(f"  {chr(0x2713)} 服务可用，已注册工具: {info}")

    # Step 2: 读取测试数据
    print(f"\n  [Step 2] 从数据库读取测试 sessionId ...")
    try:
        cnx = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("SELECT agent_login_session_id FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%' ORDER BY created_time")
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

        print(f"  {chr(0x2713)} 读到 {len(rows)} 条 test_ 记录")
        for k, v in sid_map.items():
            print(f"    {k:10s} -> {v[:24]}...")
        if len(sid_map) < 3:
            print(f"  {chr(0x26a0)} 缺少测试数据，请先运行: python init_userbindtest_data.py")
            sys.exit(1)
    except Exception as e:
        print(f"  {chr(0x2717)} 读取失败: {e}")
        print(f"    请确认 MySQL 已启动并运行: python init_userbindtest_data.py")
        sys.exit(1)

    # Step 3: 执行测试
    print(f"\n  {'='*56}")
    print(f"  开始测试（共 5 个场景）")
    print(f"  {'='*56}")

    run_test("场景 1: bound   有效session + 已绑定员工",  sid_map["bound"],   "bound")
    run_test("场景 2: unbound 有效session + 未绑定员工",  sid_map["unbound"], "unbound")
    run_test("场景 3: invalid sessionId 不存在",          "nonexistent_session_00001", "invalid")
    run_test("场景 4: expired session 已过期",             sid_map["expired"], "invalid")
    run_test("场景 5: no_header 不传 agentLoginSessionId", None,              "invalid")

    print(f"\n  {'='*56}")
    print(f"  所有测试完成")
    print(f"  {'='*56}")
    print()


if __name__ == "__main__":
    main()
