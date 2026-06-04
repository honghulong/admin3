import urllib.request, json, uuid

BASE = 'http://localhost:9099/admin3'

# 先登录获取 token
login_url = BASE + '/login'
login_body = json.dumps({'username':'admin','password':'123456','captcha':'','rememberMe':False}).encode()
login_req = urllib.request.Request(login_url, data=login_body, headers={'Content-Type':'application/json'}, method='POST')
login_resp = urllib.request.urlopen(login_req)
login_result = json.loads(login_resp.read().decode())
token = login_result['token']
print('Token:', token[:20] + '...')

# 测试附件上传（不带 reimbursementId）
boundary = '----' + str(uuid.uuid4())
body = b''
body += ('--' + boundary + '\r\n').encode()
body += 'Content-Disposition: form-data; name="file"; filename="test.png"\r\n'.encode()
body += 'Content-Type: image/png\r\n\r\n'.encode()
body += b'fake-image-data'
body += ('\r\n--' + boundary + '--\r\n').encode()

upload_url = BASE + '/attachments/upload'
upload_req = urllib.request.Request(upload_url, data=body, headers={
    'Content-Type': 'multipart/form-data; boundary=' + boundary,
    'Authorization': 'Bearer ' + token
}, method='POST')
try:
    upload_resp = urllib.request.urlopen(upload_req)
    print('Upload success:', upload_resp.read().decode())
except urllib.error.HTTPError as e:
    print('Upload error:', e.code, e.read().decode())
