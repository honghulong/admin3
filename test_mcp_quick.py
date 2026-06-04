#!/usr/bin/env python
"""快速测试报销 MCP 的三个 Tool"""
import sys, urllib.request, json, base64, os

# 设置输出编码
sys.stdout.reconfigure(encoding='utf-8')

BASE = 'http://localhost:9099/admin3'
MCP = BASE + '/xiaoyi-mcp/reimbursement/message'
HEADERS = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream',
           'appId': 'admin3-leave-app', 'apiKey': 'admin3-leave-api-key-2026'}

def call(method, params=None):
    body = json.dumps({'jsonrpc': '2.0', 'id': 1, 'method': method, 'params': params or {}}).encode()
    req = urllib.request.Request(MCP, data=body, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        return {'error': e.code, 'body': e.read().decode()}
    except Exception as e:
        return {'error': str(e)}

def extract_text(result):
    if 'result' in result and 'content' in result['result']:
        for c in result['result']['content']:
            if c.get('type') == 'text':
                return c['text']
    return None

# 1. tools/list
print('=' * 60)
print('  1. tools/list')
print('=' * 60)
r = call('tools/list')
tools = r.get('result', {}).get('tools', [])
for t in tools:
    print(f"  Tool: {t['name']}")
    print(f"        描述: {t['description']}")
print()

# 2. query_reimbursements_by_username
print('=' * 60)
print('  2. query_reimbursements_by_username (admin)')
print('=' * 60)
r = call('tools/call', {'name': 'query_reimbursements_by_username', 'arguments': {'username': 'admin'}})
text = extract_text(r)
print(text if text else json.dumps(r, indent=2, ensure_ascii=False))
print()

# 3. ocr_upload_reimbursement
print('=' * 60)
print('  3. ocr_upload_reimbursement')
print('=' * 60)
img_path = 'test-invoice.png'
if not os.path.exists(img_path):
    print(f"  [ERR] 文件不存在: {img_path}")
    sys.exit(1)

with open(img_path, 'rb') as f:
    b64 = base64.b64encode(f.read()).decode()
print(f"  图片: {img_path} ({len(b64)} bytes base64)")
print()

r = call('tools/call', {'name': 'ocr_upload_reimbursement', 'arguments': {'imageBase64': b64, 'filename': 'test-invoice.png'}})
text = extract_text(r)
if text:
    print(text)
else:
    print(json.dumps(r, indent=2, ensure_ascii=False))
