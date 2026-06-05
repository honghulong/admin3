#!/usr/bin/env python
"""用 requests 测试 MCP 调用"""
import requests, json, base64, time, sys

sys.stdout.reconfigure(encoding='utf-8')

with open('test-invoice.png', 'rb') as f:
    b64 = base64.b64encode(f.read()).decode()
print(f'图片 base64 大小: {len(b64)} bytes')

params = {
    'name': 'ocr_upload_reimbursement',
    'arguments': {
        'imageBase64': b64,
        'filename': 'test-invoice.png',
        'applicantName': 'admin',
    }
}
body = json.dumps({'jsonrpc':'2.0','id':1,'method':'tools/call','params':params})
print(f'请求体大小: {len(body)} bytes')

headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json, text/event-stream',
    'appId': 'admin3-leave-app',
    'apiKey': 'admin3-leave-api-key-2026',
}
print(f'发送请求...')
sys.stdout.flush()
t0 = time.time()
resp = requests.post(
    'http://localhost:9099/admin3/xiaoyi-mcp/reimbursement/message',
    data=body, headers=headers, timeout=600
)
elapsed = time.time() - t0
print(f'状态: {resp.status_code}, 耗时: {elapsed:.1f}s')
data = resp.json()
if 'result' in data and 'content' in data['result']:
    for c in data['result']['content']:
        if c.get('type') == 'text':
            print(f'响应(前2000): {c["text"][:2000]}')
else:
    print(f'响应: {json.dumps(data, ensure_ascii=False)[:500]}')
