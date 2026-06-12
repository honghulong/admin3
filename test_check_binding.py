#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 XiaoYi MCP binding 服务（check_binding_status + bind_employee）

前置条件:
  1) python init_userbindtest_data.py    # 先插入测试数据
  2) .\start.ps1                 # 再启动服务
  3) python test_check_binding.py

check_binding_status 测试场景（5 个）:
  bound    - 有效 session + 已绑定员工
  unbound  - 有效 session + 未绑定员工
  invalid  - sessionId 不存在
  expired  - session 已过期
  no_header - 不传 agentLoginSessionId header

bind_employee 测试场景（3 个）:
  正常绑定  - 用 unbound session 绑定有效员工ID
  重复绑定  - 已绑定的 session 再次绑定
  无效员工ID - 绑定不存在的员工ID
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
MCP_BINDING_PATH = f"{CONTEXT_PATH}/xiaoyi-mcp/binding/message"
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


def call_mcp(session_id, tool_name="check_binding_status", arguments=None):
    """调用 MCP 工具（模拟小艺：sessionId 放 Header）"""
    headers = dict(AUTH_HEADERS)
    if session_id:
        headers["agentLoginSessionId"] = session_id

    if arguments is None:
        arguments = {}

    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": tool_name, "arguments": arguments},
    })

    conn = http.client.HTTPConnection(BASE_URL, timeout=10)
    try:
        conn.request("POST", MCP_BINDING_PATH, body, headers)
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


def call_bind_employee(session_id, x_employee_id):
    """调用 bind_employee MCP 工具"""
    return call_mcp(session_id, "bind_employee", {"xEmployeeId": x_employee_id})


def check_server():
    """检查 MCP 服务"""
    headers = dict(AUTH_HEADERS)
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}})
    conn = http.client.HTTPConnection(BASE_URL, timeout=5)
    try:
        conn.request("POST", MCP_BINDING_PATH, body, headers)
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
    print(f"\n  [Step 1] 检查服务 http://{BASE_URL}{MCP_BINDING_PATH}")
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

    # ============================================================
    # Step 4: 测试 bind_employee
    # ============================================================
    print(f"\n")
    print(f"  {'='*56}")
    print(f"  bind_employee 测试")
    print(f"  {'='*56}")

    unbound_sid = sid_map["unbound"]

    # 从数据库查一个有效的 xEmployeeId
    emp_id = None
    try:
        cnx = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("SELECT x_employee_id FROM sys_user WHERE x_employee_id IS NOT NULL LIMIT 1")
        row = cursor.fetchone()
        cursor.close()
        cnx.close()
        if row:
            emp_id = row[0]
    except Exception as e:
        print(f"  [bind_employee] 查询员工ID失败: {e}")

    # 场景 6: 正常绑定
    print()
    print(f"  {'='*56}")
    print(f"  [场景 6: bind_employee 正常绑定]")
    print(f"  {'='*56}")
    if emp_id:
        t0 = time.time()
        data, http_code = call_bind_employee(unbound_sid, emp_id)
        elapsed = time.time() - t0
        text, is_ok = parse_result(data)
        http_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
        print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
        print(f"  Header: agentLoginSessionId={unbound_sid[:24]}...")
        print(f"  参数: xEmployeeId={emp_id}")
        print(f"  返回:")
        for line in text.split("\\n"):
            print(f"    {line}")
        if "绑定成功" in text:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x2713)} 绑定成功")
        else:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x26a0)} 绑定失败")
    else:
        print(f"  [跳过] 未找到有 xEmployeeId 的用户")

    # 场景 7: 重复绑定（已绑定的 session 再次绑定）
    print()
    print(f"  {'='*56}")
    print(f"  [场景 7: bind_employee 重复绑定]")
    print(f"  {'='*56}")
    if emp_id:
        t0 = time.time()
        data, http_code = call_bind_employee(unbound_sid, emp_id)
        elapsed = time.time() - t0
        text, is_ok = parse_result(data)
        http_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
        print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
        print(f"  Header: agentLoginSessionId={unbound_sid[:24]}...")
        print(f"  参数: xEmployeeId={emp_id}")
        print(f"  返回:")
        for line in text.split("\\n"):
            print(f"    {line}")
        if "绑定成功" in text:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x2713)} 重复绑定成功（更新绑定关系）")
        else:
            print(f"  {chr(0x2500)*4} 验证: {chr(0x26a0)} 重复绑定结果异常")

    # 场景 8: 无效员工ID
    print()
    print(f"  {'='*56}")
    print(f"  [场景 8: bind_employee 无效员工ID]")
    print(f"  {'='*56}")
    t0 = time.time()
    data, http_code = call_bind_employee(unbound_sid, "NONEXISTENT_EMP_999")
    elapsed = time.time() - t0
    text, is_ok = parse_result(data)
    http_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
    print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  Header: agentLoginSessionId={unbound_sid[:24]}...")
    print(f"  参数: xEmployeeId=NONEXISTENT_EMP_999")
    print(f"  返回:")
    for line in text.split("\\n"):
        print(f"    {line}")
    if "员工ID不存在" in text:
        print(f"  {chr(0x2500)*4} 验证: {chr(0x2713)} 正确拒绝无效员工ID")
    else:
        print(f"  {chr(0x2500)*4} 验证: {chr(0x26a0)} 未按预期拒绝")

    # 验证绑定后 check_binding_status 返回 bound
    print()
    print(f"  {'='*56}")
    print(f"  [验证] 绑定后 check_binding_status")
    print(f"  {'='*56}")
    t0 = time.time()
    data, http_code = call_mcp(unbound_sid)
    elapsed = time.time() - t0
    text, is_ok = parse_result(data)
    http_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
    print(f"  HTTP: {http_code} {http_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  返回:")
    for line in text.split("\\n"):
        print(f"    {line}")
    if "bound" in text:
        print(f"  {chr(0x2500)*4} 验证: {chr(0x2713)} 绑定后状态变为 bound")
    else:
        print(f"  {chr(0x2500)*4} 验证: {chr(0x26a0)} 状态未变为 bound")

    print(f"\n  {'='*56}")
    print(f"  所有测试完成")
    print(f"  {'='*56}")
    print()


if __name__ == "__main__":
    main()
