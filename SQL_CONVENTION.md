# SQL 脚本规范

## 三类脚本

| 分类 | 文件名 | 作用 | 执行时机 |
|------|--------|------|----------|
| **全量初始化** | `init_db.sql` | 建库 + 所有 DDL + 所有数据，保持最新最完整 | 新环境搭建 |
| **数据恢复** | `import_data.sql` | 仅数据 INSERT（表已存在时） | 测试数据重置 |
| **增量迁移** | `V{序号}__{描述}.sql` | 仅包含本次变更（ALTER / INSERT / UPDATE） | 每次有 DDL 或数据变更 |

## 文件清单（当前）

```
项目根目录/
├── init_db.sql                # [核心] 全量初始化，永远保持最新
├── import_data.sql             # [辅助] 仅数据恢复
├── bak/                        # 废弃脚本归档，不再使用
│   ├── data.sql
│   ├── reimbursement-module.sql
│   ├── reimbursement-data.sql
│   └── reset_root.sql
└── admin3-server/src/main/resources/
    └── V001__reimbursement_ocr_fields.sql   # [迁移] 报销模块 OCR 字段
```

## 增量迁移规则

1. **每次变更都做两件事**：
   - 更新 `init_db.sql`（保持全量脚本最新）
   - 新建 `V{下一序号}__{描述}.sql`（仅包含增量 SQL）

2. **编号规则**：V001、V002、V003... 按时间顺序递增，不要跳号

3. **执行顺序**：按编号从小到大依次执行

4. **文件位置**：统一放在 `admin3-server/src/main/resources/` 下

## 导入命令（防乱码）

**所有 SQL 脚本都必须指定 utf8 编码导入：**
```bash
mysql -u root -p --default-character-set=utf8 < init_db.sql
mysql -u root -p --default-character-set=utf8 admin3 < import_data.sql
mysql -u root -p --default-character-set=utf8 admin3 < V002__xxx.sql
```

**MySQL 容器配置要求：**
```bash
docker run ... mysql:5.7 --character-set-server=utf8 --collation-server=utf8_general_ci
```

> 如果不加 `--default-character-set=utf8`，MySQL 客户端默认使用 `latin1` 连接，
> 插入的中文会被双倍编码（UTF-8 → Latin-1 → UTF-8），导致界面乱码。

## 常见场景

| 场景 | 操作 |
|------|------|
| 加字段/改表 | 更新 init_db.sql + 新建 V00X__xxx.sql |
| 加新测试数据 | 更新 init_db.sql + 更新 import_data.sql |
| 数据库被删 | `mysql --default-character-set=utf8 < init_db.sql` |
| 测试数据乱了 | `mysql --default-character-set=utf8 admin3 < import_data.sql` |
| 界面出现乱码 | 检查导入时是否漏了 `--default-character-set=utf8` |

## 提醒

- 不要手动改数据库！所有变更必须通过 SQL 脚本记录下来
- 不要直接改数据库！所有变更必须通过 SQL 脚本记录下来
- 不要直接改数据库！所有变更必须通过 SQL 脚本记录下来
- 导入 SQL 脚本必须加 `--default-character-set=utf8`，否则中文变乱码
- MySQL 容器启动必须加 `--character-set-server=utf8 --collation-server=utf8_general_ci`
