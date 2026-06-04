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

result = multipart_post('http://localhost:8765/ocr', 'test-invoice.png')

# 输出到 stdout
print(json.dumps(result, indent=2, ensure_ascii=False))

# 也写到文件
with open('ocr_output2.json', 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=2, ensure_ascii=False)

print('\n--- summary ---')
print('invoice_fields keys:', list(result.get('invoice_fields', {}).keys()))
print('status:', result.get('status'))
print('ocr_raw count:', len(result.get('ocr_raw', [])))
print('tables count:', len(result.get('tables', [])))
