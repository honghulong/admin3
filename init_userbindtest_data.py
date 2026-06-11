#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
插入 MCP check_binding_status 测试数据
"""
import mysql.connector
import uuid
from datetime import datetime, timedelta

DB = {"host": "127.0.0.1", "port": 3306, "user": "root", "password": "123456", "database": "admin3", "charset": "utf8mb4"}

cnx = mysql.connector.connect(**DB)
cursor = cnx.cursor()

# 1. 确保测试用户
username = "test_xiaoyi_staff"
cursor.execute("SELECT id, x_employee_id FROM user WHERE username = %s", (username,))
row = cursor.fetchone()
if row:
    user_id, emp_id = row[0], row[1]
    print(f"[user] 已存在: id={user_id}, x_employee_id={emp_id}")
else:
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("INSERT INTO user (username, gender, state, created_time, x_employee_id) VALUES (%s, 0, 0, %s, %s)",
                   (username, now, "EMP-TEST-001"))
    cnx.commit()
    user_id = cursor.lastrowid
    emp_id = "EMP-TEST-001"
    print(f"[user] 新建: id={user_id}, x_employee_id={emp_id}")

# 2. 清理旧的测试数据
cursor.execute("DELETE FROM xiao_yi_user_session WHERE agent_login_session_id LIKE 'test_%'")
cnx.commit()
print("[clean] 已清理旧 test_ 开头的 session")

# 3. 插入 3 条测试 session
now = datetime.now()
sessions = [
    ("test_bound_" + uuid.uuid4().hex[:16],   user_id,  24,  "bound"),
    ("test_unbound_" + uuid.uuid4().hex[:14],  None,     24,  "unbound"),
    ("test_expired_" + uuid.uuid4().hex[:14],  user_id, -1,  "expired"),
]

for sid, uid, hours, label in sessions:
    expire = now + timedelta(hours=hours)
    open_id = f"hw_openid_{sid[-12:]}"
    phone = "13800138000" if uid else None
    sql = """INSERT INTO xiao_yi_user_session
             (agent_login_session_id, phone_number, huawei_open_id, user_id, created_time, expire_time)
             VALUES (%s, %s, %s, %s, %s, %s)"""
    cursor.execute(sql, (sid, phone, open_id, uid, now, expire))
    cnx.commit()
    uid_str = str(uid) if uid else "NULL"
    print(f"[insert] {label:8s}  {sid}  uid={uid_str}  expire={expire.strftime('%m-%d %H:%M')}")

cursor.close()
cnx.close()
print()
print("完成！现在可以运行 python test_check_binding.py 进行测试")
