#!/usr/bin/env python
"""
报销业务 MCP 服务 - 完整测试脚本
================================
测试报销 MCP 的三个 Tool:
  1. ocr_upload_reimbursement  - OCR识别并创建草稿
  2. confirm_reimbursement     - 确认并提交正式报销单
  3. query_reimbursements_by_username - 按用户名查询报销单

用法:
  python test_reimbursement_mcp.py                          # 完整测试流程
  python test_reimbursement_mcp.py --list-tools             # 仅列出工具
  python test_reimbursement_mcp.py --query 用户名           # 仅查询报销单
  python test_reimbursement_mcp.py --image D:\发票.jpg      # 测试OCR+确认完整流程
  python test_reimbursement_mcp.py --ocr-test D:\发票.jpg   # 直接测试OCR HTTP接口
  python test_reimbursement_mcp.py --help                   # 显示帮助

前置条件:
  1. admin3 服务已启动 (http://localhost:9099/admin3)
  2. OCR 服务已启动 (http://localhost:8765)

OCR 服务接口:
  - HTTP REST: POST http://localhost:8765/ocr -F "file=@发票.png"
  - HTTP REST: POST http://localhost:8765/ocr -H "Content-Type: application/json" -d '{"image_path":"D:\\发票.png"}'
  - 健康检查:  GET http://localhost:8765/health
"""

import urllib.request
import json
import re
import sys
import os
import base64

# ============================================================
# 配置
# ============================================================
ADMIN3_BASE_URL = "http://localhost:9099/admin3"
MCP_ENDPOINT = f"{ADMIN3_BASE_URL}/xiaoyi-mcp/reimbursement/message"
OCR_HTTP_URL = "http://localhost:8765"

# 测试图片路径（已拷贝到项目根目录）
TEST_INVOICE_PNG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-invoice.png")
TEST_INVOICE_JPG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-invoice2.jpg")

HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "appId": "admin3-leave-app",
    "apiKey": "admin3-leave-api-key-2026"
}

# ============================================================
# MCP 调用工具函数
# ============================================================

def call_mcp(endpoint, method, params=None, headers=None):
    """调用 MCP 服务"""
    if headers is None:
        headers = HEADERS
    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params or {}
    }).encode('utf-8')
    req = urllib.request.Request(endpoint, data=body, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return {"error": e.code, "body": e.read().decode('utf-8')}
    except Exception as e:
        return {"error": str(e)}


def call_reimbursement_tool(tool_name, arguments):
    """调用报销 MCP 的 Tool"""
    return call_mcp(MCP_ENDPOINT, "tools/call", {
        "name": tool_name,
        "arguments": arguments
    })


def list_reimbursement_tools():
    """列出报销 MCP 的所有 Tool"""
    return call_mcp(MCP_ENDPOINT, "tools/list")


# OCR 服务已改为纯 HTTP REST 接口 (POST /ocr, GET /health)
# 不再使用 MCP 协议调用


# ============================================================
# 结果提取工具
# ============================================================

def extract_text(result):
    """从 MCP 返回结果中提取文本内容"""
    if "result" in result and "content" in result["result"]:
        for content in result["result"]["content"]:
            if content.get("type") == "text":
                return content["text"]
    return None


def extract_reimbursement_id(result):
    """从返回结果中提取报销单 ID"""
    text = extract_text(result)
    if text:
        match = re.search(r'ID:\s*(\d+)', text)
        if match:
            return int(match.group(1))
    return None


def is_error(result):
    """判断 MCP 调用是否出错"""
    if "error" in result:
        return True
    if "result" in result and "isError" in result["result"]:
        return result["result"].get("isError", False)
    return False


def get_error_message(result):
    """获取错误信息"""
    if "error" in result:
        return str(result["error"])
    text = extract_text(result)
    if text:
        return text
    return "未知错误"


# ============================================================
# 打印工具
# ============================================================

def print_box(title):
    width = 60
    print()
    print("=" * width)
    print(f"  {title}")
    print("=" * width)


def print_result(label, result):
    print(f"\n{'─' * 60}")
    print(f"  {label}")
    print(f"{'─' * 60}")
    if is_error(result):
        print(f"  [ERR] {get_error_message(result)}")
    else:
        text = extract_text(result)
        if text:
            for line in text.strip().split("\n"):
                print(f"  {line}")
        else:
            print(json.dumps(result, indent=2, ensure_ascii=False))


def print_json(label, data):
    print(f"\n{'─' * 60}")
    print(f"  {label}")
    print(f"{'─' * 60}")
    print(json.dumps(data, indent=2, ensure_ascii=False))


# ============================================================
# 服务检查
# ============================================================

def check_server(url, name):
    """检查服务是否在运行"""
    try:
        req = urllib.request.Request(url)
        urllib.request.urlopen(req, timeout=5)
        return True
    except urllib.error.HTTPError:
        return True  # HTTP 错误说明服务在运行
    except Exception:
        return False


def check_ocr_server():
    """检查 OCR 服务 (HTTP REST 接口)"""
    return check_server("http://localhost:8765/health", "OCR")


def check_admin3_server():
    """检查 admin3 服务"""
    return check_server(f"{ADMIN3_BASE_URL}/xiaoyi-mcp/reimbursement/message", "admin3")


# ============================================================
# 测试用例
# ============================================================

def test_list_tools():
    """测试1: 列出报销 MCP 的工具"""
    print_box("测试1: 列出报销 MCP 工具 (tools/list)")
    result = list_reimbursement_tools()
    print_json("报销 MCP 工具列表", result)
    return not is_error(result)


def test_query_reimbursements(username="admin"):
    """测试2: 按用户名查询报销单"""
    print_box(f"测试2: 查询报销单 - 用户名: {username}")
    result = call_reimbursement_tool("query_reimbursements_by_username", {
        "username": username
    })
    print_result(f"查询结果", result)
    return not is_error(result)


def test_ocr_upload(image_path=None):
    """
    测试3: OCR 上传识别
    如果没有传图片路径，使用一个测试用的 Base64 图片数据
    """
    print_box("测试3: OCR 上传识别 (ocr_upload_reimbursement)")

    if image_path and os.path.exists(image_path):
        # 从文件读取图片
        with open(image_path, "rb") as f:
            image_base64 = base64.b64encode(f.read()).decode()
        filename = os.path.basename(image_path)
        print(f"  图片文件: {image_path}")
        print(f"  文件名: {filename}")
        print(f"  Base64 长度: {len(image_base64)} 字符")
    else:
        # 没有图片文件，先测试 OCR 服务是否可用
        print("  [WARN] 未指定图片路径，跳过 OCR 上传测试")
        print("  用法: python test_reimbursement_mcp.py --image D:\\发票.jpg")
        return None

    result = call_reimbursement_tool("ocr_upload_reimbursement", {
        "imageBase64": image_base64,
        "filename": filename
    })
    print_result("OCR 识别结果", result)

    if not is_error(result):
        reimbursement_id = extract_reimbursement_id(result)
        if reimbursement_id:
            print(f"\n  >>> 草稿报销单 ID: {reimbursement_id}")
            return reimbursement_id
    return None


def test_confirm_reimbursement(reimbursement_id, category="other"):
    """测试4: 确认并提交报销单"""
    print_box(f"测试4: 确认报销单 - ID: {reimbursement_id}")

    result = call_reimbursement_tool("confirm_reimbursement", {
        "reimbursementId": reimbursement_id,
        "category": category
    })
    print_result("确认结果", result)
    return not is_error(result)


def test_ocr_http_direct(image_path):
    """直接测试 OCR 服务的 HTTP REST 接口 (POST /ocr)"""
    print_box("直接测试 OCR HTTP 接口 (POST /ocr)")

    if not os.path.exists(image_path):
        print(f"  [ERR] 文件不存在: {image_path}")
        return False

    import subprocess

    print(f"  图片: {image_path}")
    print(f"  请求: curl -X POST http://localhost:8765/ocr -F \"file=@发票.png\"")
    print()

    # 用 urllib 模拟 multipart/form-data 上传
    boundary = "----TestBoundary" + str(int(__import__('time').time()))
    line_sep = "\r\n"

    with open(image_path, "rb") as f:
        image_data = f.read()

    filename = os.path.basename(image_path)

    body = []
    body.append(f"--{boundary}{line_sep}")
    body.append(f'Content-Disposition: form-data; name="file"; filename="{filename}"{line_sep}')
    body.append(f"Content-Type: application/octet-stream{line_sep}")
    body.append(f"{line_sep}")
    body.append(f"{line_sep}")

    body_bytes = "".join(body).encode('utf-8')
    body_bytes += image_data
    body_bytes += f"{line_sep}--{boundary}--{line_sep}".encode('utf-8')

    req = urllib.request.Request(
        "http://localhost:8765/ocr",
        data=body_bytes,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            result = json.loads(resp.read().decode('utf-8'))
            print_json("OCR 识别结果", result)

            if result.get("status") == "error":
                print(f"  [ERR] {result.get('error')}")
                return False

            fields = result.get("invoice_fields", {})
            if fields:
                print("  --- 关键字段 ---")
                for k, v in fields.items():
                    print(f"    {k}: {v}")

            text = result.get("ocr_text", "")
            if text:
                print()
                print("  --- OCR 文本（前300字） ---")
                print(f"  {text[:300]}")

            print()
            print("  [OK] OCR HTTP 接口测试通过！")
            return True
    except urllib.error.HTTPError as e:
        print(f"  [ERR] HTTP {e.code}: {e.read().decode('utf-8')}")
        return False
    except Exception as e:
        print(f"  [ERR] {e}")
        return False


def test_full_flow(image_path=None):
    """
    测试5: 完整流程 - OCR识别 → 确认提交 → 查询验证
    """
    print_box("测试5: 完整流程测试")

    # Step 1: OCR 识别
    print("\n  >>> Step 1: OCR 识别上传...")
    reimbursement_id = test_ocr_upload(image_path)
    if reimbursement_id is None:
        print("  [ERR] OCR 识别失败，终止流程")
        return False

    # Step 2: 确认提交
    print(f"\n  >>> Step 2: 确认提交报销单 (ID: {reimbursement_id})...")
    if not test_confirm_reimbursement(reimbursement_id, "travel"):
        print("  [ERR] 确认提交失败")
        return False

    # Step 3: 查询验证
    print(f"\n  >>> Step 3: 查询验证...")
    test_query_reimbursements("admin")

    print()
    print("=" * 60)
    print("  [OK] 完整流程测试通过！")
    print("=" * 60)
    return True


# ============================================================
# 主函数
# ============================================================

def print_help():
    print(__doc__)


def main():
    args = [a.lower() for a in sys.argv[1:]]

    if "--help" in args or "-h" in args:
        print_help()
        return

    # 提取图片路径
    image_path = None
    if "--image" in args:
        idx = args.index("--image")
        if idx + 1 < len(sys.argv[1:]):
            image_path = sys.argv[1:][idx + 1]

    # 提取 OCR 直接测试图片路径
    ocr_test_path = None
    if "--ocr-test" in args:
        idx = args.index("--ocr-test")
        if idx + 1 < len(sys.argv[1:]):
            ocr_test_path = sys.argv[1:][idx + 1]

    # 提取查询用户名
    query_username = None
    if "--query" in args:
        idx = args.index("--query")
        if idx + 1 < len(sys.argv[1:]):
            query_username = sys.argv[1:][idx + 1]

    print()
    print("=" * 60)
    print("  报销业务 MCP 服务 - 测试脚本")
    print(f"  MCP 端点: {MCP_ENDPOINT}")
    print("=" * 60)
    print()

    # 检查服务
    print("  检查服务状态...")
    admin3_ok = check_admin3_server()
    ocr_ok = check_ocr_server()
    print(f"  admin3 服务: {'[OK]' if admin3_ok else '[ERR]'}")
    print(f"  OCR 服务:    {'[OK]' if ocr_ok else '[ERR]'}")

    if not admin3_ok:
        print("\n  [ERR] admin3 服务未启动！请先启动:")
        print("         .\\mvnw.cmd spring-boot:run -pl admin3-server")
        return

    if not ocr_ok:
        print("\n  [WARN] OCR 服务未启动！OCR 相关测试将失败")
        print("         启动: cd D:\\hongmengProjects\\ocr-invoice-mcp && uv run python ocr_server.py")
        print()

    # 执行测试
    if "--list-tools" in args:
        test_list_tools()
    elif query_username:
        test_query_reimbursements(query_username)
    elif ocr_test_path:
        test_ocr_http_direct(ocr_test_path)
    elif image_path:
        test_full_flow(image_path)
    else:
        # 默认：列出工具 + 查询 + OCR 完整流程（使用本地测试图片）
        test_list_tools()
        test_query_reimbursements("admin")
        test_query_reimbursements("刘小波")

        if ocr_ok:
            # 优先用 PNG，没有则用 JPG
            test_img = TEST_INVOICE_PNG if os.path.exists(TEST_INVOICE_PNG) else TEST_INVOICE_JPG
            if os.path.exists(test_img):
                print()
                print(f"  >>> 使用本地测试图片: {test_img}")
                test_full_flow(test_img)
            else:
                print()
                print("  [WARN] 未找到本地测试图片，跳过 OCR 测试")
                print(f"         请放置图片到: {TEST_INVOICE_PNG}")
        else:
            print()
            print("  [INFO] OCR 服务未启动，跳过 OCR 相关测试")

    print()
    print("=" * 60)
    print("  测试完成")
    print("=" * 60)
    print()
    print("用法参考:")
    print("  python test_reimbursement_mcp.py                          # 基础测试 + OCR完整流程")
    print("  python test_reimbursement_mcp.py --list-tools             # 仅列出工具")
    print("  python test_reimbursement_mcp.py --query 用户名           # 查询报销单")
    print("  python test_reimbursement_mcp.py --image D:\\发票.jpg      # 指定图片完整流程")
    print("  python test_reimbursement_mcp.py --ocr-test D:\\发票.jpg   # 直接测试OCR HTTP接口")


if __name__ == "__main__":
    main()
