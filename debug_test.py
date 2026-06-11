import http.client, json, sys
sys.stdout.reconfigure(encoding="utf-8")

# Direct test - use raw socket to see what headers we're sending
sid = "test_bound_366cae21fd50412f"
body = json.dumps({"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"check_binding_status","arguments":{}}})

# Test 1: with header
print("=== Test 1: With agentLoginSessionId header ===")
headers = {
    "Content-Type": "application/json",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026",
    "agentLoginSessionId": sid,
}
conn = http.client.HTTPConnection("localhost", 9099, timeout=10)
conn.request("POST", "/admin3/xiaoyi-mcp/auth/message", body, headers)
r = conn.getresponse()
print(f"HTTP {r.status}")
data = json.loads(r.read().decode("utf-8"))
if "result" in data and "content" in data["result"]:
    for c in data["result"]["content"]:
        if c.get("type") == "text":
            print(c["text"])
conn.close()
