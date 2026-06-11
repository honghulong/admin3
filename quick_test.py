import http.client, json, sys, time, mysql.connector
sys.stdout.reconfigure(encoding="utf-8")

HOST = "localhost:9099"
PATH = "/admin3/xiaoyi-mcp/auth/message"
HEADERS = {"Content-Type":"application/json","Accept":"application/json, text/event-stream","appId":"admin3-leave-app","apiKey":"admin3-leave-api-key-2026"}
DB = {"host":"127.0.0.1","port":3306,"user":"root","password":"123456","database":"admin3","charset":"utf8mb4"}

def call(sid):
    h = dict(HEADERS)
    if sid: h["agentLoginSessionId"] = sid
    body = json.dumps({"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"check_binding_status","arguments":{}}})
    conn = http.client.HTTPConnection("localhost",9099,timeout=10)
    try:
        conn.request("POST", PATH, body, h)
        r = conn.getresponse()
        return json.loads(r.read().decode("utf-8")), r.status
    except Exception as e:
        return {"error":str(e)}, 0
    finally:
        conn.close()

# Check tools
h = dict(HEADERS)
body = json.dumps({"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}})
conn = http.client.HTTPConnection("localhost",9099,timeout=10)
conn.request("POST", PATH, body, h)
r = conn.getresponse()
data = json.loads(r.read().decode("utf-8"))
tools = data.get("result",{}).get("tools",[])
print(f"Registered tools: {[t['name'] for t in tools]}")
conn.close()

# Read test sessions
cnx = mysql.connector.connect(**DB)
cur = cnx.cursor()
cur.execute("SELECT agent_login_session_id FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%' ORDER BY created_time")
rows = cur.fetchall()
cur.close()
cnx.close()
sid_map = {}
for (sid,) in rows:
    if sid.startswith("test_bound_") and "bound" not in sid_map: sid_map["bound"] = sid
    elif sid.startswith("test_unbound_") and "unbound" not in sid_map: sid_map["unbound"] = sid
    elif sid.startswith("test_expired_") and "expired" not in sid_map: sid_map["expired"] = sid
print(f"Test sessions: bound={sid_map.get('bound','?')[:16]}... unbound={sid_map.get('unbound','?')[:16]}... expired={sid_map.get('expired','?')[:16]}...")

# Run tests
cases = [
    ("bound", sid_map.get("bound")),
    ("unbound", sid_map.get("unbound")),
    ("invalid", "nonexistent_session_00001"),
    ("expired", sid_map.get("expired")),
    ("no_header", None),
]
for label, sid in cases:
    print(f"\n=== {label} ===")
    t0 = time.time()
    d, code = call(sid)
    el = time.time()-t0
    sd = (sid[:20]+"...") if sid and len(sid)>24 else (sid or "null")
    print(f"HTTP {code}  sid={sd}  {el:.2f}s")
    if "result" in d and "content" in d["result"]:
        for c in d["result"]["content"]:
            if c.get("type")=="text":
                for line in c["text"].split("\n"):
                    print(f"  {line}")
    elif "error" in d:
        print(f"  ERROR: {d['error']}")
    else:
        print(f"  RAW: {json.dumps(d, ensure_ascii=False)[:200]}")
