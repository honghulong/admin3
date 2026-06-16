#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
插入 XiaoYi MCP 测试数据（使用 sid 作为设备标识）

插入 3 条测试 session:
  bound   - sid 存在 + 已绑定员工（有效 24h）
  unbound - sid 存在 + 未绑定员工（有效 24h）
  expired - sid 存在 + 已绑定员工（已过期 -1h）

自动迁移：检查并添加 sid / session_id 列
"""
import mysql.connector
import uuid
from datetime import datetime, timedelta

DB = {"host": "127.0.0.1", "port": 3306, "user": "root", "password": "123456", "database": "admin3", "charset": "utf8mb4"}

cnx = mysql.connector.connect(**DB)
cursor = cnx.cursor()

# 0. 自动迁移：检查并添加 sid / session_id 列
cursor.execute("SHOW COLUMNS FROM xiao_yi_user_session LIKE 'sid'")
if not cursor.fetchone():
    print("[migrate] 添加 sid 列...")
    cursor.execute("ALTER TABLE xiao_yi_user_session ADD COLUMN sid VARCHAR(255) NULL COMMENT '小艺设备标识(deviceInfo.sid)', ADD UNIQUE INDEX idx_xiao_yi_user_session_sid (sid)")
    cursor.execute("UPDATE xiao_yi_user_session SET sid = agent_login_session_id WHERE sid IS NULL")
    cnx.commit()
    print("[migrate] sid 列添加完成")
cursor.execute("SHOW COLUMNS FROM xiao_yi_user_session LIKE 'session_id'")
if not cursor.fetchone():
    print("[migrate] 添加 session_id 列...")
    cursor.execute("ALTER TABLE xiao_yi_user_session ADD COLUMN session_id VARCHAR(255) NULL COMMENT '小艺会话ID(session.sessionId)', ADD INDEX idx_xiao_yi_user_session_session_id (session_id)")
    cursor.execute("UPDATE xiao_yi_user_session SET session_id = agent_login_session_id WHERE session_id IS NULL")
    cnx.commit()
    print("[migrate] session_id 列添加完成")

# 0.1 检查并删除 agentLoginSessionId 的旧唯一约束（sid 已成为主标识）
cursor.execute("SHOW INDEX FROM xiao_yi_user_session WHERE Column_name = 'agent_login_session_id' AND Non_unique = 0")
old_uk = cursor.fetchone()
if old_uk:
    uk_name = old_uk[2]
    print(f"[migrate] 删除 agentLoginSessionId 的唯一约束: {uk_name}...")
    cursor.execute(f"ALTER TABLE xiao_yi_user_session DROP INDEX `{uk_name}`")
    cnx.commit()
    print("[migrate] 唯一约束已删除")

# 0.2 检查并修改 agentLoginSessionId 允许 NULL（新 session 不再需要此字段）
cursor.execute("SHOW COLUMNS FROM xiao_yi_user_session LIKE 'agent_login_session_id'")
col_info = cursor.fetchone()
if col_info and col_info[2] == 'NO':  # Null 列为 'NO' 表示 NOT NULL
    print("[migrate] agentLoginSessionId 改为允许 NULL...")
    cursor.execute("ALTER TABLE xiao_yi_user_session MODIFY COLUMN agent_login_session_id VARCHAR(255) NULL COMMENT '华为授权sessionID（暂未使用）'")
    cnx.commit()
    print("[migrate] agentLoginSessionId 已改为允许 NULL")

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

# 2. 清理旧的测试数据（sid 和 agentLoginSessionId 都以 test_ 开头）
cursor.execute("DELETE FROM xiao_yi_user_session WHERE sid LIKE 'test_%' OR agent_login_session_id LIKE 'test_%'")
cnx.commit()
print("[clean] 已清理旧 test_ 开头的 session")

# 3. 插入 3 条测试 session（使用 sid 作为主标识）
now = datetime.now()
test_sessions = [
    {
        "label": "bound",
        "sid": "test_sid_bound_" + uuid.uuid4().hex[:16],
        "session_id": "test_session_bound_" + uuid.uuid4().hex[:12],
        "user_id": user_id,
        "hours": 24,
    },
    {
        "label": "unbound",
        "sid": "test_sid_unbound_" + uuid.uuid4().hex[:14],
        "session_id": "test_session_unbound_" + uuid.uuid4().hex[:12],
        "user_id": None,
        "hours": 24,
    },
    {
        "label": "expired",
        "sid": "test_sid_expired_" + uuid.uuid4().hex[:14],
        "session_id": "test_session_expired_" + uuid.uuid4().hex[:12],
        "user_id": user_id,
        "hours": -1,
    },
]

for s in test_sessions:
    expire = now + timedelta(hours=s["hours"])
    open_id = f"hw_openid_{s['sid'][-12:]}"
    phone = "13800138000" if s["user_id"] else None
    sql = """INSERT INTO xiao_yi_user_session
             (sid, session_id, agent_login_session_id, phone_number, huawei_open_id, user_id, created_time, expire_time)
             VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"""
    agent_sid = f"test_{s['label']}_" + uuid.uuid4().hex[:16]
    cursor.execute(sql, (s["sid"], s["session_id"], agent_sid, phone, open_id, s["user_id"], now, expire))
    cnx.commit()
    uid_str = str(s["user_id"]) if s["user_id"] else "NULL"
    print(f"[insert] {s['label']:8s}  sid={s['sid']}")
    print(f"         session_id={s['session_id']}  uid={uid_str}  expire={expire.strftime('%m-%d %H:%M')}")

cursor.close()
cnx.close()
print()
print("完成！现在可以运行 python test_check_binding.py 进行测试")
