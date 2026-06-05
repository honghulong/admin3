#!/usr/bin/env python
"""用 subprocess + curl 测试 MCP 调用"""
import subprocess, json, base64, time, sys, os

sys.stdout.reconfigure(encoding='utf-8')

with open('test-invoice.png', 'rb') as f:
    b64 = base64.b64encode(f.read()).decode()

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
print(f'发送请求 (curl)...')
sys.stdout.flush()
t0 = time.time()

with open('_req_body.json', 'w', encoding='utf-8') as f:
    f.write(body)

result = subprocess.run([
    'curl', '-s', '-o', '_resp_body.json', '-w', '%{http_code}',
    '-X', 'POST',
    'http://localhost:9099/admin3/xiaoyi-mcp/reimbursement/message',
    '-H', 'Content-Type: application/json',
    '-H', 'Accept: application/json, text/event-stream',
    '-H', 'appId: admin3-leave-app',
    '-H', 'apiKey: admin3-leave-api-key-2026',
    '-d', '@_req_body.json',
    '--max-time', '600'
], capture_output=True, text=True, timeout=600)

elapsed = time.time() - t0
print(f'状态码: {result.stdout}, 耗时: {elapsed:.1f}s')

if result.stdout.strip() == '200':
    with open('_resp_body.json', 'r', encoding='utf-8') as f:
        data = json.load(f)
    if 'result' in data and 'content' in data['result']:
        for c in data['result']['content']:
            if c.get('type') == 'text':
                text = c['text']
                print(f'响应(前2000): {text[:2000]}')
    else:
        print(f'响应: {json.dumps(data, ensure_ascii=False)[:500]}')
else:
    print(f'curl stderr: {result.stderr[:500]}')

# 清理
for f in ['_req_body.json', '_resp_body.json']:
    try: os.unlink(f)
    except: pass
