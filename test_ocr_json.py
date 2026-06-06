#!/usr/bin/env python
"""获取 OCR 完整 JSON 输出"""
import sys, urllib.request, json, os, http.client, mimetypes, urllib.parse

sys.stdout.reconfigure(encoding='utf-8')

def multipart_post(url, file_path):
    boundary = '----' + os.urandom(16).hex()
    with open(file_path, 'rb') as f:
        file_data = f.read()
    filename = os.path.basename(file_path)
    body = []
    body.append('--' + boundary)
    body.append(f'Content-Disposition: form-data; name="file"; filename="{filename}"')
    body.append('Content-Type: ' + (mimetypes.guess_type(filename)[0] or 'application/octet-stream'))
    body.append('')
    body.append('')
    head = '\r\n'.join(body).encode('utf-8')
    tail = ('\r\n--' + boundary + '--\r\n').encode('utf-8')
    body_bytes = head + file_data + tail
    parsed = urllib.parse.urlparse(url)
    conn = http.client.HTTPConnection(parsed.hostname, parsed.port)
    conn.request('POST', parsed.path, body_bytes, {
        'Content-Type': 'multipart/form-data; boundary=' + boundary,
        'Content-Length': str(len(body_bytes))
    })
    resp = conn.getresponse()
    return json.loads(resp.read().decode('utf-8'))

result = multipart_post('http://localhost:8765/ocr', 'd:/hongmengProjects/admin3/test-invoice.png')

# 原始 JSON 写到文件
with open('ocr_output2.json', 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=2, ensure_ascii=False)
print(f'[OK] 原始结果已保存到 ocr_output2.json')
print()

# 检查状态
if result.get('status') == 'error':
    print(f'[ERR] {result.get("error")}')
    sys.exit(1)

# 输出 OCR 文本块数量
blocks = result.get('ocr_raw', [])
print(f'OCR 文本块: {len(blocks)} 个')
print()

# 输出 OCR 文本内容
text = result.get('ocr_text', '')
if text:
    print('--- OCR 文本内容 ---')
    for line in text.split('\n')[:30]:
        print(f'  {line}')

# 输出 KIE 关键字段
fields = result.get('invoice_fields', {})
if fields:
    print()
    print('--- KIE 关键字段 ---')
    for k, v in fields.items():
        print(f'  {k}: {v}')
else:
    print()
    print('--- KIE 关键字段 ---')
    print('  (无结构化字段)')

print()
print('--- summary ---')
print(f'  status: {result.get("status")}')
print(f'  ocr_raw count: {len(blocks)}')
print(f'  invoice_fields keys: {list(fields.keys())}')
print(f'  tables count: {len(result.get("tables", []))}')
