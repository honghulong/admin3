#!/usr/bin/env python
"""
报销 MCP 快速测试脚本

功能：测试报销模块 MCP 服务的三个核心 Tool：
  1. tools/list                          — 列出所有可用的 MCP Tool
  2. ocr_upload_reimbursement            — 上传发票图片进行 OCR 识别并创建草稿报销单
  3. confirm_reimbursement               — 用户确认后，将草稿报销单提交为正式报销单（进入审批流程）
  4. query_reimbursements_by_username    — 按用户名查询报销单列表

用法：
  python test_mcp_quick.py

前置条件：
  - 服务已启动（http://localhost:9099/admin3）
  - 同级目录下存在 test-invoice.png 测试图片
"""
import sys
import json
import base64
import os
import http.client
import re
import mysql.connector

# 设置输出编码
sys.stdout.reconfigure(encoding='utf-8')

# ============================================================
# 数据库配置
# ============================================================
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'admin3',
    'charset': 'utf8',
}


def get_db_connection():
    """创建数据库连接"""
    return mysql.connector.connect(**DB_CONFIG)


def query_reimbursement_stats():
    """查询 reimbursement 表的统计信息：最大 ID 和总记录数"""
    conn = get_db_connection()
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT MAX(id) AS max_id, COUNT(*) AS total_count FROM reimbursement")
        row = cursor.fetchone()
        cursor.close()
        return row
    finally:
        conn.close()


# ============================================================
# 配置
# ============================================================
BASE = 'http://localhost:9099/admin3'
MCP = BASE + '/xiaoyi-mcp/reimbursement/message'
HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json, text/event-stream',
    'appId': 'admin3-leave-app',
    'apiKey': 'admin3-leave-api-key-2026',
}


# ============================================================
# MCP 调用工具函数
# ============================================================
def call(method, params=None, timeout=600):
    """发送 JSON-RPC 请求到 MCP 端点（使用 http.client 避免 urllib 阻塞问题）"""
    body = json.dumps({
        'jsonrpc': '2.0',
        'id': 1,
        'method': method,
        'params': params or {},
    })
    conn = http.client.HTTPConnection('localhost', 9099, timeout=timeout)
    try:
        conn.request('POST', '/admin3/xiaoyi-mcp/reimbursement/message', body, HEADERS)
        resp = conn.getresponse()
        return json.loads(resp.read().decode('utf-8'))
    except Exception as e:
        return {'error': str(e)}
    finally:
        conn.close()


def extract_text(result):
    """从 MCP 响应中提取 text 类型的内容"""
    if 'result' in result and 'content' in result['result']:
        for c in result['result']['content']:
            if c.get('type') == 'text':
                return c['text']
    return None


# ============================================================
# 测试前：记录数据库初始状态
# ============================================================
print('=' * 60)
print('  [数据库] 测试前 reimbursement 表状态')
print('=' * 60)
stats_before = query_reimbursement_stats()
print(f"  最大 ID: {stats_before['max_id']}")
print(f"  总记录数: {stats_before['total_count']}")
print()

# ============================================================
# 1. tools/list — 列出所有可用的 MCP Tool
# ============================================================
print('=' * 60)
print('  1. tools/list')
print('=' * 60)
r = call('tools/list')
tools = r.get('result', {}).get('tools', [])
for t in tools:
    print(f"  Tool: {t['name']}")
    print(f"        描述: {t['description']}")
print()

# ============================================================
# 2. ocr_upload_reimbursement — OCR 识别并创建草稿报销单
#    必填参数: imageBase64, filename, applicantName
# ============================================================
print('=' * 60)
print('  2. ocr_upload_reimbursement')
print('=' * 60)
img_path = 'test-invoice.png'
if not os.path.exists(img_path):
    print(f"  [ERR] 文件不存在: {img_path}")
    sys.exit(1)

with open(img_path, 'rb') as f:
    b64 = base64.b64encode(f.read()).decode()
print(f"  图片: {img_path} ({len(b64)} bytes base64)")
print()

r = call('tools/call', {
    'name': 'ocr_upload_reimbursement',
    'arguments': {
        'imageBase64': b64,
        'filename': 'test-invoice.png',
        'applicantName': 'admin',  # 必填，报销申请人姓名
    },
})
text = extract_text(r)
if text:
    print(text)
else:
    print(json.dumps(r, indent=2, ensure_ascii=False))
print()

# ============================================================
# 3. confirm_reimbursement — 确认草稿，提交为正式报销单
#    必填参数: reimbursementId, category
#    可选参数: title, amount
# ============================================================
print('=' * 60)
print('  3. confirm_reimbursement')
print('=' * 60)

# 从步骤 2 的返回文本中提取草稿 ID
draft_id_match = re.search(r'草稿报销单 ID:\s*(\d+)', text) if text else None
if draft_id_match:
    draft_id = draft_id_match.group(1)
    print(f"  从 OCR 结果中提取到草稿 ID: {draft_id}")
    print()

    r = call('tools/call', {
        'name': 'confirm_reimbursement',
        'arguments': {
            'reimbursementId': draft_id,
            'category': 'office',
            'title': '[MCP] 办公设备采购报销 - 测试',
        },
    })
    text3 = extract_text(r)
    if text3:
        print(text3)
    else:
        print(json.dumps(r, indent=2, ensure_ascii=False))
else:
    print("  [跳过] 未能从 OCR 结果中提取草稿 ID")
print()

# ============================================================
# 4. query_reimbursements_by_username — 查询报销单
#    必填参数: username
# ============================================================
print('=' * 60)
print('  4. query_reimbursements_by_username (admin)')
print('=' * 60)
r = call('tools/call', {
    'name': 'query_reimbursements_by_username',
    'arguments': {'username': 'admin'},
})
text = extract_text(r)
print(text if text else json.dumps(r, indent=2, ensure_ascii=False))
print()

# ============================================================
# 测试后：验证数据库最终状态
# ============================================================
print('=' * 60)
print('  [数据库] 测试后 reimbursement 表状态')
print('=' * 60)
stats_after = query_reimbursement_stats()
print(f"  最大 ID: {stats_after['max_id']}")
print(f"  总记录数: {stats_after['total_count']}")
print()

# 对比验证
print('=' * 60)
print('  [数据库] 变更对比')
print('=' * 60)
added_count = stats_after['total_count'] - stats_before['total_count']
print(f"  新增记录数: {added_count}")
if added_count > 0:
    print(f"  新增 ID 范围: {stats_before['max_id'] + 1} ~ {stats_after['max_id']}")
print()
