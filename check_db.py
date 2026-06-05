#!/usr/bin/env python
"""查询数据库验证报销单数据"""
import pymysql, json

conn = pymysql.connect(host='127.0.0.1', user='root', password='123456', database='admin3', charset='utf8mb4')
cur = conn.cursor()

# 查表名
cur.execute("SHOW TABLES LIKE '%reimburs%'")
tables = [r[0] for r in cur.fetchall()]
print(f'报销相关表: {tables}')

# 查最新报销单
table_name = 'reimbursement'
if table_name in tables:
    cur.execute(f"DESCRIBE {table_name}")
    print(f'\n表 {table_name} 结构:')
    for r in cur.fetchall():
        print(f'  {r[0]:30s} {r[1]:20s}')
    cur.execute(f"SELECT * FROM {table_name} ORDER BY id DESC LIMIT 3")
    col_names = [desc[0] for desc in cur.description]
    print(f'\n列名: {col_names}')
    for row in cur.fetchall():
        print(f'\n--- ID={row[0]} ---')
        for i, col in enumerate(col_names):
            val = str(row[i])[:200] if row[i] is not None else 'NULL'
            print(f'  {col}: {val}')
    for r in cur.fetchall():
        print(f'\n--- ID={r[0]} ---')
        print(f'  标题: {r[1]}')
        print(f'  金额: {r[2]}')
        print(f'  状态: {r[3]}')
        print(f'  说明: {r[4]}')
        print(f'  有OCR原始数据: {r[5]}')
        print(f'  OCR原始数据(前300): {r[6]}')
        print(f'  发票代码: {r[7]}')
        print(f'  开票日期: {r[8]}')
        print(f'  发票金额: {r[9]}')
        print(f'  购买方: {r[10]}')
        print(f'  销售方: {r[11]}')
else:
    print(f'表 {table_name} 不存在')

cur.close()
conn.close()
