import urllib.request
import json

BASE_URL = "https://28wh9253sf76.vicp.fun/admin3/xiaoyi-mcp/message"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026"
}

body = json.dumps({
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
}).encode('utf-8')

req = urllib.request.Request(BASE_URL, data=body, headers=HEADERS)
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        result = json.loads(resp.read().decode('utf-8'))
        tools = result.get('result', {}).get('tools', [])
        for t in tools:
            print(f"Tool: {t['name']}")
            print(f"  描述: {t['description']}")
            print()
except Exception as e:
    print(f"Error: {e}")
