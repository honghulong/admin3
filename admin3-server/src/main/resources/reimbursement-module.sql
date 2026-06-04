-- ============================================================
-- 报销管理模块 SQL（独立执行）
-- 使用前请确认：
--   1. reimbursement / attachment / approval_log 表已存在
--      （JPA 会自动建表）
--   2. resource 表已有 id=1 的根节点
--   3. role 表已有 id=1（超级管理员）和 id=2（开发者）
--   4. sys_dict 表已有 id=1~4 的数据
-- ============================================================

-- 关闭外键约束检查
set foreign_key_checks = 0;

-- ============================================================
-- 1. 权限资源
-- ============================================================
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (43, 'Money', '报销管理', null, 'reimbursement:view', 0, '/reimbursements', 1);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (44, null, '查看报销', null, 'reimbursement:view', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (45, null, '新增报销', null, 'reimbursement:create', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (46, null, '修改报销', null, 'reimbursement:update', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (47, null, '删除报销', null, 'reimbursement:delete', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (48, null, '提交报销', null, 'reimbursement:submit', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (49, null, '审批报销', null, 'reimbursement:approve', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (50, null, '撤回报销', null, 'reimbursement:recall', 1, null, 43);

-- ============================================================
-- 2. 角色权限分配
-- ============================================================
-- 超级管理员（role_id=1）
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 43);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 44);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 45);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 46);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 47);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 48);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 49);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 50);

-- 开发者（role_id=2）
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 43);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 44);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 45);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 46);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 47);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 48);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 49);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 50);

-- ============================================================
-- 3. 字典数据
-- ============================================================
-- 报销类别字典
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (5, 'reimbursement_category', '报销类别', '报销申请的类别');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (16, 5, '差旅', 'travel', 1, '出差差旅费');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (17, 5, '办公', 'office', 2, '办公用品采购');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (18, 5, '交通', 'transport', 3, '交通费用');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (19, 5, '餐饮', 'catering', 4, '餐饮费用');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (20, 5, '其他', 'other', 5, '其他费用');

-- 报销状态字典
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (6, 'reimbursement_status', '报销状态', '报销申请的状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (21, 6, '草稿', 'draft', 1, '草稿状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (22, 6, '待审批', 'pending', 2, '待审批状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (23, 6, '已通过', 'approved', 3, '已审批通过');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (24, 6, '已退回', 'rejected', 4, '已审批退回');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (25, 6, '已撤回', 'recalled', 5, '已撤回');

-- 发票类型字典
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (7, 'reimbursement_invoice_type', '发票类型', '发票的类型分类');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (26, 7, '增值税专用发票', 'special', 1, '增值税专用发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (27, 7, '增值税普通发票', 'normal', 2, '增值税普通发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (28, 7, '电子发票', 'electronic', 3, '电子发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (29, 7, '定额发票', 'fixed', 4, '定额发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (30, 7, '机动车销售发票', 'vehicle', 5, '机动车销售统一发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (31, 7, '其他', 'other', 6, '其他类型发票');

-- 发票状态字典
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (8, 'reimbursement_invoice_status', '发票状态', '发票的状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (32, 8, '正常', 'normal', 1, '发票正常');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (33, 8, '已作废', 'voided', 2, '发票已作废');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (34, 8, '已红冲', 'red_charged', 3, '发票已红冲');

-- ============================================================
-- 4. 测试数据
-- ============================================================
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (1, '6月出差上海差旅费', 'travel', 1280.50, '6月1日-3日上海出差差旅费', 'pending', 2, '管理员', '2026-06-01 10:00:00.000000', '2026-06-01 10:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (2, '办公用品采购', 'office', 320.00, '采购打印纸、笔、文件夹等', 'approved', 3, '张三', '2026-06-01 14:00:00.000000', '2026-06-02 09:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (3, '打车费用报销', 'transport', 85.00, '5月28日客户拜访打车', 'draft', 2, '管理员', '2026-06-02 11:00:00.000000', '2026-06-02 11:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (4, '客户招待餐饮费', 'catering', 560.00, '招待重要客户午餐', 'rejected', 3, '张三', '2026-05-30 16:00:00.000000', '2026-05-31 10:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (5, '端午节福利采购', 'other', 2000.00, '端午节员工福利采购', 'pending', 2, '管理员', '2026-05-25 09:00:00.000000', '2026-05-25 09:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (6, '6月出差深圳差旅费', 'travel', 3500.00, '6月5日-7日深圳出差差旅费', 'pending', 202, '呼保义宋江', '2026-06-03 08:00:00.000000', '2026-06-03 08:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (7, '项目团建餐饮费', 'catering', 1800.00, '项目第一阶段完成团建聚餐', 'draft', 203, '玉麒麟卢俊义', '2026-06-02 16:00:00.000000', '2026-06-02 16:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (8, '服务器采购', 'office', 15000.00, '采购开发服务器一台', 'pending', 207, '豹子头林冲', '2026-06-01 09:30:00.000000', '2026-06-01 09:30:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (9, '客户拜访交通费', 'transport', 230.00, '6月2日拜访客户往返打车', 'approved', 214, '花和尚鲁智深', '2026-06-02 10:00:00.000000', '2026-06-03 09:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (10, '技术书籍采购', 'other', 450.00, '采购Spring Cloud相关技术书籍', 'recalled', 215, '行者武松', '2026-05-28 14:00:00.000000', '2026-05-29 11:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (11, '成都出差差旅费', 'travel', 2200.00, '5月20日-22日成都出差', 'approved', 218, '青面兽杨志', '2026-05-19 09:00:00.000000', '2026-05-23 10:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (12, '团队下午茶', 'catering', 320.00, '周五团队下午茶', 'rejected', 223, '黑旋风李逵', '2026-06-02 15:00:00.000000', '2026-06-03 08:30:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (13, '出差北京高铁票', 'transport', 580.00, '5月15日北京出差高铁票', 'approved', 237, '浪子燕青', '2026-05-14 10:00:00.000000', '2026-05-16 14:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (14, '显示器采购', 'office', 1299.00, '采购27寸4K显示器一台', 'pending', 240, '病尉迟孙立', '2026-06-03 11:00:00.000000', '2026-06-03 11:00:00.000000');
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at) VALUES (15, '云服务器费用', 'other', 299.00, '6月阿里云服务器费用', 'draft', 202, '呼保义宋江', '2026-06-01 00:00:00.000000', '2026-06-01 00:00:00.000000');

-- 审批日志测试数据
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (1, 2, 'submit', 3, '张三', null, '2026-06-01 14:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (2, 2, 'approve', 2, '管理员', '同意报销', '2026-06-02 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (3, 4, 'submit', 3, '张三', null, '2026-05-30 16:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (4, 4, 'reject', 2, '管理员', '发票金额与实际申请不一致，请重新核实', '2026-05-31 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (5, 9, 'submit', 214, '花和尚鲁智深', null, '2026-06-02 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (6, 9, 'approve', 2, '管理员', '核实无误，同意报销', '2026-06-03 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (7, 10, 'submit', 215, '行者武松', null, '2026-05-28 14:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (8, 10, 'recall', 215, '行者武松', '重复提交了，先撤回', '2026-05-29 11:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (9, 11, 'submit', 218, '青面兽杨志', null, '2026-05-19 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (10, 11, 'approve', 2, '管理员', '同意报销', '2026-05-23 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (11, 12, 'submit', 223, '黑旋风李逵', null, '2026-06-02 15:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (12, 12, 'reject', 2, '管理员', '下午茶费用请从团建经费中支出', '2026-06-03 08:30:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (13, 13, 'submit', 237, '浪子燕青', null, '2026-05-14 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (14, 13, 'approve', 2, '管理员', '同意', '2026-05-16 14:00:00.000000');

-- 开启外键约束检查
set foreign_key_checks = 1;
