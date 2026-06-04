#!/usr/bin/env python
"""快速测试报销 MCP 的三个 Tool，结果写入文件"""
import sys, urllib.request, json, base64, os

sys.stdout.reconfigure(encoding='utf-8')
lines = []

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
lines.append('=' * 60)
lines.append('  1. tools/list')
lines.append('=' * 60)
r = call('tools/list')
tools = r.get('result', {}).get('tools', [])
for t in tools:
    lines.append("  Tool: " + t['name'])
    lines.append("        描述: " + t['description'])
lines.append('')

# 2. query
lines.append('=' * 60)
lines.append('  2. query_reimbursements_by_username (admin)')
lines.append('=' * 60)
r = call('tools/call', {'name': 'query_reimbursements_by_username', 'arguments': {'username': 'admin'}})
text = extract_text(r)
lines.append(text if text else json.dumps(r, indent=2, ensure_ascii=False))
lines.append('')

# 3. ocr
lines.append('=' * 60)
lines.append('  3. ocr_upload_reimbursement')
lines.append('=' * 60)
img_path = 'test-invoice.png'
if not os.path.exists(img_path):
    lines.append('  [ERR] 文件不存在: ' + img_path)
else:
    with open(img_path, 'rb') as f:
        b64 = base64.b64encode(f.read()).decode()
    lines.append('  图片: ' + img_path + ' (' + str(len(b64)) + ' bytes base64)')
    lines.append('')
    r = call('tools/call', {'name': 'ocr_upload_reimbursement', 'arguments': {'imageBase64': b64, 'filename': 'test-invoice.png'}})
    text = extract_text(r)
    if text:
        lines.append(text)
    else:
        lines.append(json.dumps(r, indent=2, ensure_ascii=False))

with open('test_result2.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))
print('done, wrote test_result2.txt')
