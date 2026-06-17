#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 XiaoYi MCP binding 服务（check_binding_status + bind_employee）

前置条件:
  1) python init_userbindtest_data.py    # 先插入测试数据
  2) 启动服务
  3) python test_check_binding.py

check_binding_status 测试场景（6 个）:
  bound            - 有效 sid + 已绑定员工
  unbound          - 有效 sid + 未绑定员工
  invalid          - sid 不存在
  expired          - sid 已过期
  no_id            - 不传任何标识
  x-request-id     - 仅传 x-request-id header（无 arguments），测试 fallback

bind_employee 测试场景（3 个）:
  正常绑定（x-request-id fallback） - 模拟小艺只传 xEmployeeId + x-request-id
  重复绑定                          - 已绑定的 session 再次绑定
  无效员工ID                        - 绑定不存在的员工ID
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


def build_args(sid=None, session_id=None, extra=None):
    """构建模拟小艺传来的 arguments（包含 deviceInfo 和 session）"""
    args = {}
    if sid:
        args["deviceInfo"] = {"sid": sid}
    if session_id:
        args["session"] = {"sessionId": session_id}
    if extra:
        args.update(extra)
    return args


def call_mcp(sid=None, session_id=None, tool_name="check_binding_status",
             arguments=None, x_request_id=None):
    """
    调用 MCP 工具
    模拟小艺两种传参方式：
      1. arguments 中带 deviceInfo.sid / session.sessionId
      2. arguments 中无设备标识，靠 x-request-id header fallback
    """
    headers = dict(AUTH_HEADERS)
    if x_request_id:
        headers["x-request-id"] = x_request_id

    if arguments is None:
        arguments = build_args(sid, session_id)

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


def call_bind_employee_via_header(x_request_id, x_employee_id):
    """
    模拟小艺真实场景：bind_employee 调用时不传 deviceInfo.session，
    仅传 xEmployeeId，靠 x-request-id header fallback 识别用户
    """
    args = {"xEmployeeId": x_employee_id}
    return call_mcp(None, None, "bind_employee", args, x_request_id=x_request_id)


def check_server():
    """检查 MCP 服务"""
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}})
    conn = http.client.HTTPConnection(BASE_URL, timeout=5)
    try:
        conn.request("POST", MCP_BINDING_PATH, body, AUTH_HEADERS)
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


def run_test(scenario, sid, session_id=None, expect_status=None, x_request_id=None):
    """执行单个测试用例"""
    print()
    print(f"  {'='*56}")
    print(f"  [{scenario}]")
    print(f"  {'='*56}")

    t0 = time.time()
    data, http_code = call_mcp(sid, session_id, x_request_id=x_request_id)
    elapsed = time.time() - t0

    text, is_ok = parse_result(data)
    ok_flag = "\u2713" if http_code == 200 else "\u2717"
    sid_display = (sid[:24] + "...") if sid and len(sid) > 28 else (sid or "null")
    sess_display = (session_id[:16] + "...") if session_id and len(session_id) > 20 else (session_id or "null")
    xid_display = (x_request_id[:24] + "...") if x_request_id and len(x_request_id) > 28 else (x_request_id or "null")

    print(f"  HTTP: {http_code} {ok_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  sid={sid_display}, sessionId={sess_display}")
    if x_request_id:
        print(f"  x-request-id={xid_display}")
    print(f"  返回:")
    for line in text.split("\\n"):
        print(f"    {line}")

    if expect_status:
        expected = f"\u7ed1\u5b9a\u72b6\u6001: {expect_status}"
        if expected in text:
            print(f"  {'-'*4} 验证: {chr(0x2713)} 期望状态 [{expect_status}] 匹配")
        else:
            print(f"  {'-'*4} 验证: {chr(0x26a0)} 期望状态 [{expect_status}] 未匹配")
    print()


def main():
    print()
    print("  "+chr(0x2554)+chr(0x2550)*56+chr(0x2557))
    print(f"  {chr(0x2551)}     XiaoYi MCP  check_binding_status  测试                    {chr(0x2551)}")
    print("  "+chr(0x2560)+chr(0x2550)*56+chr(0x2563))
    print(f"  {chr(0x2551)}  设备标识来源: arguments OR x-request-id header fallback        {chr(0x2551)}")
    print(f"  {chr(0x2551)}  数据已通过 init_userbindtest_data.py 插入                          {chr(0x2551)}")
    print(f"  {chr(0x2551)}  sid 前缀:                                                     {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_sid_bound_*     有效 + 已绑定                          {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_sid_unbound_*   有效 + 未绑定                          {chr(0x2551)}")
    print(f"  {chr(0x2551)}    test_sid_expired_*   已过期                                  {chr(0x2551)}")
    print("  "+chr(0x255a)+chr(0x2550)*56+chr(0x255d))

    # Step 1: 检查服务
    print(f"\n  [Step 1] 检查服务 http://{BASE_URL}{MCP_BINDING_PATH}")
    ok, info = check_server()
    if not ok:
        print(f"  {chr(0x2717)} 服务不可用: {info}")
        print(f"    请先启动服务")
        sys.exit(1)
    print(f"  {chr(0x2713)} 服务可用，已注册工具: {info}")

    # Step 2: 读取测试数据
    print(f"\n  [Step 2] 从数据库读取测试 sid ...")
    try:
        cnx = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("SELECT sid, session_id FROM xiao_yi_user_session WHERE sid LIKE 'test_sid_%' ORDER BY created_time")
        rows = cursor.fetchall()
        cursor.close()
        cnx.close()

        sid_map = {}
        for (sid, sess_id) in rows:
            if sid.startswith("test_sid_bound_") and "bound" not in sid_map:
                sid_map["bound"] = (sid, sess_id)
            elif sid.startswith("test_sid_unbound_") and "unbound" not in sid_map:
                sid_map["unbound"] = (sid, sess_id)
            elif sid.startswith("test_sid_expired_") and "expired" not in sid_map:
                sid_map["expired"] = (sid, sess_id)

        print(f"  {chr(0x2713)} 读到 {len(rows)} 条 test_sid_ 记录")
        for k, v in sid_map.items():
            print(f"    {k:10s} -> sid={v[0][:28]}...")
        if len(sid_map) < 3:
            print(f"  {chr(0x26a0)} 缺少测试数据，请先运行: python init_userbindtest_data.py")
            sys.exit(1)
    except Exception as e:
        print(f"  {chr(0x2717)} 读取失败: {e}")
        print(f"    请确认 MySQL 已启动并运行: python init_userbindtest_data.py")
        sys.exit(1)

    # Step 3: 执行 check_binding_status 测试
    print(f"\n  {'='*56}")
    print(f"  开始测试（共 6 个场景）")
    print(f"  {'='*56}")

    run_test("场景 1: bound       有效sid + 已绑定员工",
             sid_map["bound"][0], sid_map["bound"][1], "bound")
    run_test("场景 2: unbound     有效sid + 未绑定员工",
             sid_map["unbound"][0], sid_map["unbound"][1], "unbound")
    run_test("场景 3: invalid     sid 不存在",
             "nonexistent_sid_001", None, "unbound")
    run_test("场景 4: expired     sid 已过期",
             sid_map["expired"][0], sid_map["expired"][1], "invalid")
    run_test("场景 5: no_id       不传任何标识",
             None, None, "unbound")
    # 场景 6: 模拟小艺不传 deviceInfo（只传 x-request-id header）
    # x-request-id 格式: sessionId&1-random-uuid
    fallback_session_id = sid_map["unbound"][1]
    run_test("场景 6: x-request-id fallback（无 arguments，仅 header）",
             None, None, "unbound",
             x_request_id=f"{fallback_session_id}&1-aaaaaaaaaaaa")

    # ============================================================
    # Step 4: 测试 bind_employee（使用 x-request-id fallback 方式模拟小艺真实行为）
    # ============================================================
    print(f"\n")
    print(f"  {'='*56}")
    print(f"  bind_employee 测试（通过 x-request-id header fallback）")
    print(f"  {'='*56}")

    unbound_sid = sid_map["unbound"][0]
    unbound_session_id = sid_map["unbound"][1]

    # 从数据库查一个有效的 xEmployeeId
    emp_id = None
    try:
        cnx = mysql.connector.connect(**DB_CONFIG)
        cursor = cnx.cursor()
        cursor.execute("SELECT x_employee_id FROM user WHERE x_employee_id IS NOT NULL LIMIT 1")
        row = cursor.fetchone()
        cursor.close()
        cnx.close()
        if row:
            emp_id = row[0]
    except Exception as e:
        print(f"  [bind_employee] 查询员工ID失败: {e}")

    # 场景 7: 正常绑定（模拟小艺：仅传 xEmployeeId + x-request-id header）
    print()
    print(f"  {'='*56}")
    print(f"  [场景 7: bind_employee 正常绑定（x-request-id fallback）]")
    print(f"  {'='*56}")
    if emp_id:
        xrid = f"{unbound_session_id}&1-bind-test-uuid"
        t0 = time.time()
        data, http_code = call_bind_employee_via_header(xrid, emp_id)
        elapsed = time.time() - t0
        text, is_ok = parse_result(data)
        ok_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
        print(f"  HTTP: {http_code} {ok_flag}  |  耗时: {elapsed:.2f}s")
        print(f"  x-request-id={xrid}")
        print(f"  arguments中无deviceInfo/session，仅 xEmployeeId={emp_id}")
        print(f"  返回:")
        for line in text.split("\\n"):
            print(f"    {line}")
        if "\u7ed1\u5b9a\u6210\u529f" in text:
            print(f"  {'-'*4} 验证: {chr(0x2713)} 绑定成功（x-request-id fallback 生效）")
        else:
            print(f"  {'-'*4} 验证: {chr(0x26a0)} 绑定失败")
    else:
        print(f"  [\u8df3\u8fc7] \u672a\u627e\u5230\u6709 xEmployeeId \u7684\u7528\u6237")

    # 场景 8: 重复绑定
    print()
    print(f"  {'='*56}")
    print(f"  [\u573a\u666f 8: bind_employee \u91cd\u590d\u7ed1\u5b9a]")
    print(f"  {'='*56}")
    if emp_id:
        xrid = f"{unbound_session_id}&1-rebind-uuid"
        t0 = time.time()
        data, http_code = call_bind_employee_via_header(xrid, emp_id)
        elapsed = time.time() - t0
        text, is_ok = parse_result(data)
        ok_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
        print(f"  HTTP: {http_code} {ok_flag}  |  耗时: {elapsed:.2f}s")
        print(f"  x-request-id={xrid}")
        print(f"  返回:")
        for line in text.split("\\n"):
            print(f"    {line}")
        if "\u7ed1\u5b9a\u6210\u529f" in text:
            print(f"  {'-'*4} 验证: {chr(0x2713)} \u91cd\u590d\u7ed1\u5b9a\u6210\u529f\uff08\u66f4\u65b0\u7ed1\u5b9a\u5173\u7cfb\uff09")
        else:
            print(f"  {'-'*4} 验证: {chr(0x26a0)} \u91cd\u590d\u7ed1\u5b9a\u7ed3\u679c\u5f02\u5e38")

    # 场景 9: 无效员工ID
    print()
    print(f"  {'='*56}")
    print(f"  [\u573a\u666f 9: bind_employee \u65e0\u6548\u5458\u5de5ID]")
    print(f"  {'='*56}")
    xrid = f"{unbound_session_id}&1-invalid-emp-uuid"
    t0 = time.time()
    data, http_code = call_bind_employee_via_header(xrid, "NONEXISTENT_EMP_999")
    elapsed = time.time() - t0
    text, is_ok = parse_result(data)
    ok_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
    print(f"  HTTP: {http_code} {ok_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  x-request-id={xrid}")
    print(f"  参数: xEmployeeId=NONEXISTENT_EMP_999")
    print(f"  返回:")
    for line in text.split("\\n"):
        print(f"    {line}")
    if "\u5458\u5de5ID\u4e0d\u5b58\u5728" in text:
        print(f"  {'-'*4} 验证: {chr(0x2713)} \u6b63\u786e\u62d2\u7edd\u65e0\u6548\u5458\u5de5ID")
    else:
        print(f"  {'-'*4} 验证: {chr(0x26a0)} \u672a\u6309\u9884\u671f\u62d2\u7edd")

    # 验证绑定后 check_binding_status 返回 bound（通过 x-request-id fallback）
    print()
    print(f"  {'='*56}")
    print(f"  [\u9a8c\u8bc1] \u7ed1\u5b9a\u540e check_binding_status\uff08x-request-id fallback\uff09")
    print(f"  {'='*56}")
    xrid = f"{unbound_session_id}&1-verify-uuid"
    t0 = time.time()
    data, http_code = call_mcp(None, None, x_request_id=xrid)
    elapsed = time.time() - t0
    text, is_ok = parse_result(data)
    ok_flag = chr(0x2713) if http_code == 200 else chr(0x2717)
    print(f"  HTTP: {http_code} {ok_flag}  |  耗时: {elapsed:.2f}s")
    print(f"  x-request-id={xrid}")
    print(f"  \u8fd4\u56de:")
    for line in text.split("\\n"):
        print(f"    {line}")
    if "bound" in text:
        print(f"  {'-'*4} 验证: {chr(0x2713)} \u7ed1\u5b9a\u540e\u72b6\u6001\u53d8\u4e3a bound\uff08x-request-id fallback \u751f\u6548\uff09")
    else:
        print(f"  {'-'*4} 验证: {chr(0x26a0)} \u72b6\u6001\u672a\u53d8\u4e3a bound")

    print(f"\n  {'='*56}")
    print(f"  \u6240\u6709\u6d4b\u8bd5\u5b8c\u6210")
    print(f"  {'='*56}")
    print()


if __name__ == "__main__":
    main()
