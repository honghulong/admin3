import urllib.request
import json

url = "http://localhost:8080/admin3/mcp/message"
body = json.dumps({
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
}).encode('utf-8')

req = urllib.request.Request(url, data=body, headers={
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream"
})
try:
    with urllib.request.urlopen(req) as response:
        print(f"Status: {response.status}")
        print(f"Response: {json.dumps(json.loads(response.read().decode('utf-8')), indent=2, ensure_ascii=False)}")
except urllib.error.HTTPError as e:
    print(f"HTTP Error: {e.code}")
    print(f"Response: {e.read().decode('utf-8')}")
except Exception as e:
    print(f"Error: {e}")
