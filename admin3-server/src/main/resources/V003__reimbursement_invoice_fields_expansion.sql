-- ============================================================
-- 报销模块 - 发票字段扩展
-- 新增发票最大化字段结构，覆盖标准中国发票全部常见字段
-- ============================================================

-- 1. invoice 表新增字段
ALTER TABLE invoice
  ADD COLUMN amount_before_tax DECIMAL(10,2) NULL COMMENT '金额（不含税）',
  ADD COLUMN tax_rate VARCHAR(20) NULL COMMENT '税率',
  ADD COLUMN tax_amount DECIMAL(10,2) NULL COMMENT '税额',
  ADD COLUMN payee VARCHAR(100) NULL COMMENT '收款人',
  ADD COLUMN reviewer VARCHAR(100) NULL COMMENT '复核人',
  ADD COLUMN issuer VARCHAR(100) NULL COMMENT '开票人',
  ADD COLUMN buyer_address_phone VARCHAR(500) NULL COMMENT '购买方地址、电话',
  ADD COLUMN buyer_bank_info VARCHAR(500) NULL COMMENT '购买方开户行及账号',
  ADD COLUMN seller_address_phone VARCHAR(500) NULL COMMENT '销售方地址、电话',
  ADD COLUMN seller_bank_info VARCHAR(500) NULL COMMENT '销售方开户行及账号',
  ADD COLUMN remarks TEXT NULL COMMENT '备注';

-- 2. invoice_temp 表新增字段
ALTER TABLE invoice_temp
  ADD COLUMN amount_before_tax DECIMAL(10,2) NULL COMMENT '金额（不含税）',
  ADD COLUMN tax_rate VARCHAR(20) NULL COMMENT '税率',
  ADD COLUMN tax_amount DECIMAL(10,2) NULL COMMENT '税额',
  ADD COLUMN payee VARCHAR(100) NULL COMMENT '收款人',
  ADD COLUMN reviewer VARCHAR(100) NULL COMMENT '复核人',
  ADD COLUMN issuer VARCHAR(100) NULL COMMENT '开票人',
  ADD COLUMN buyer_address_phone VARCHAR(500) NULL COMMENT '购买方地址、电话',
  ADD COLUMN buyer_bank_info VARCHAR(500) NULL COMMENT '购买方开户行及账号',
  ADD COLUMN seller_address_phone VARCHAR(500) NULL COMMENT '销售方地址、电话',
  ADD COLUMN seller_bank_info VARCHAR(500) NULL COMMENT '销售方开户行及账号',
  ADD COLUMN remarks TEXT NULL COMMENT '备注';

-- 3. reimbursement 表新增字段
ALTER TABLE reimbursement
  ADD COLUMN amount_before_tax DECIMAL(10,2) NULL COMMENT '金额（不含税）',
  ADD COLUMN tax_rate VARCHAR(20) NULL COMMENT '税率',
  ADD COLUMN tax_amount DECIMAL(10,2) NULL COMMENT '税额',
  ADD COLUMN payee VARCHAR(100) NULL COMMENT '收款人',
  ADD COLUMN reviewer VARCHAR(100) NULL COMMENT '复核人',
  ADD COLUMN issuer VARCHAR(100) NULL COMMENT '开票人',
  ADD COLUMN buyer_address_phone VARCHAR(500) NULL COMMENT '购买方地址、电话',
  ADD COLUMN buyer_bank_info VARCHAR(500) NULL COMMENT '购买方开户行及账号',
  ADD COLUMN seller_address_phone VARCHAR(500) NULL COMMENT '销售方地址、电话',
  ADD COLUMN seller_bank_info VARCHAR(500) NULL COMMENT '销售方开户行及账号',
  ADD COLUMN remarks TEXT NULL COMMENT '备注';
