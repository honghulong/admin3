import http.client, json, sys, mysql.connector
sys.stdout.reconfigure(encoding="utf-8")

HOST = "localhost:9099"
PATH = "/admin3/xiaoyi-mcp/auth/message"
HEADERS = {"Content-Type":"application/json","Accept":"application/json, text/event-stream","appId":"admin3-leave-app","apiKey":"admin3-leave-api-key-2026"}

def call(args):
    body = json.dumps({"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"check_binding_status","arguments":args}})
    conn = http.client.HTTPConnection("localhost",9099,timeout=10)
    try:
        conn.request("POST", PATH, body, HEADERS)
        r = conn.getresponse()
        d = json.loads(r.read().decode("utf-8"))
        if "result" in d and "content" in d["result"]:
            for c in d["result"]["content"]:
                if c.get("type")=="text":
                    return c["text"]
        return str(d)
    except Exception as e:
        return f"ERROR: {e}"
    finally:
        conn.close()

# Read test sessions
cnx = mysql.connector.connect(**{"host":"127.0.0.1","port":3306,"user":"root","password":"123456","database":"admin3","charset":"utf8mb4"})
cur = cnx.cursor()
cur.execute("SELECT agent_login_session_id FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%' ORDER BY created_time")
rows = cur.fetchall()
cur.close(); cnx.close()
sid_map = {}
for (sid,) in rows:
    if sid.startswith("test_bound_"): sid_map["bound"] = sid
    elif sid.startswith("test_unbound_"): sid_map["unbound"] = sid
    elif sid.startswith("test_expired_"): sid_map["expired"] = sid

# Test 1: pass sessionId as argument
print("=== 传参方式（绕过Reactor线程问题）===")
for label, sid in [("bound",sid_map.get("bound")),("unbound",sid_map.get("unbound")),("expired",sid_map.get("expired"))]:
    r = call({"agentLoginSessionId": sid})
    print(f"  {label:10s}: {r.split(chr(10))[0]}")

print("\n=== 不传参（走ThreadLocal，预期可能失败）===")
for label, sid in [("bound",sid_map.get("bound")),("no_header",None)]:
    if label == "no_header":
        r = call({})
    else:
        r = call({})
    print(f"  {label:10s}: {r.split(chr(10))[0]}")

print("\n=== 不存在的sessionId ===")
r = call({"agentLoginSessionId": "nonexistent_session_00001"})
print(f"  {r.split(chr(10))[0]}")
