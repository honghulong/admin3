#!/usr/bin/env python
"""
调试 MCP 调用：测试 ocr_upload_reimbursement
用 http.client 替代 urllib，避免阻塞问题
"""
import sys
import json
import base64
import os
import http.client
import time

sys.stdout.reconfigure(encoding='utf-8')

BASE = 'http://localhost:9099/admin3'
MCP_PATH = '/admin3/xiaoyi-mcp/reimbursement/message'
HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json, text/event-stream',
    'appId': 'admin3-leave-app',
    'apiKey': 'admin3-leave-api-key-2026',
}

def call(method, params=None, timeout=300):
    """发送 JSON-RPC 请求到 MCP 端点"""
    body = json.dumps({
        'jsonrpc': '2.0',
        'id': 1,
        'method': method,
        'params': params or {},
    })
    conn = http.client.HTTPConnection('localhost', 9099, timeout=timeout)
    try:
        conn.request('POST', MCP_PATH, body, HEADERS)
        resp = conn.getresponse()
        data = json.loads(resp.read().decode('utf-8'))
        return data
    except Exception as e:
        return {'error': str(e)}
    finally:
        conn.close()

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
t0 = time.time()
r = call('tools/list')
print(f'  耗时: {time.time()-t0:.1f}s')
tools = r.get('result', {}).get('tools', [])
for t in tools:
    print(f"  Tool: {t['name']}")
print()

# 2. ocr_upload_reimbursement
print('=' * 60)
print('  2. ocr_upload_reimbursement')
print('=' * 60)
img_path = 'test-invoice.png'
if not os.path.exists(img_path):
    print(f'  [ERR] 文件不存在: {img_path}')
    sys.exit(1)

with open(img_path, 'rb') as f:
    b64 = base64.b64encode(f.read()).decode()
print(f'  图片: {img_path} ({len(b64)} bytes base64)')
print(f'  开始调用 MCP ocr_upload_reimbursement...')
print(f'  注意: 后端会同步调用 OCR 服务，可能需要较长时间')
sys.stdout.flush()

t0 = time.time()
r = call('tools/call', {
    'name': 'ocr_upload_reimbursement',
    'arguments': {
        'imageBase64': b64,
        'filename': 'test-invoice.png',
        'applicantName': 'admin',
    },
})
elapsed = time.time() - t0
print(f'  耗时: {elapsed:.1f}s')

text = extract_text(r)
if text:
    print(text[:2000])
elif 'error' in r:
    print(f'  [ERR] {r["error"]}')
else:
    print(json.dumps(r, indent=2, ensure_ascii=False)[:2000])
print()

# 3. query_reimbursements_by_username
print('=' * 60)
print('  3. query_reimbursements_by_username (admin)')
print('=' * 60)
t0 = time.time()
r = call('tools/call', {
    'name': 'query_reimbursements_by_username',
    'arguments': {'username': 'admin'},
})
print(f'  耗时: {time.time()-t0:.1f}s')
text = extract_text(r)
print(text[:2000] if text else json.dumps(r, indent=2, ensure_ascii=False)[:2000])
