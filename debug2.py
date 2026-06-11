import http.client, json, sys
sys.stdout.reconfigure(encoding="utf-8")

sid = "test_bound_366cae21fd50412f"

# Test: try sending sessionId as part of request params instead of header
body = json.dumps({
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
        "name": "check_binding_status",
        "arguments": {}
    }
})

headers = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026",
    "agentLoginSessionId": sid,
}

conn = http.client.HTTPConnection("localhost", 9099, timeout=10)
conn.request("POST", "/admin3/xiaoyi-mcp/auth/message", body, headers)
r = conn.getresponse()
raw = r.read().decode("utf-8")
print(f"HTTP {r.status}")
print(raw)
conn.close()
