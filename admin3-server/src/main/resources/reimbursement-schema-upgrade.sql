-- ============================================================
-- 报销模块 - 数据库表结构升级脚本
-- 新增 InvoiceTemp（临时发票表），用于 OCR 多次识别/确认流程
-- ============================================================

-- 0. 报销单表增加 ocr_raw_json 字段（存储最后一次有效识别的完整 OCR JSON）
-- 如果字段已存在会报错，忽略即可；或者用下面的存储过程方式
-- ALTER TABLE reimbursement ADD COLUMN ocr_raw_json MEDIUMTEXT COMMENT 'OCR 原始返回 JSON（最后一次有效识别）';

-- 安全方式：通过存储过程判断字段是否存在
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reimbursement' AND COLUMN_NAME = 'ocr_raw_json'
  ) THEN
    ALTER TABLE reimbursement ADD COLUMN ocr_raw_json MEDIUMTEXT COMMENT 'OCR 原始返回 JSON（最后一次有效识别）';
  END IF;
END //
DELIMITER ;
CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 1. 创建临时发票表
CREATE TABLE IF NOT EXISTS invoice_temp (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  reimbursement_id BIGINT COMMENT '关联的正式报销单ID',
  image_path VARCHAR(500) COMMENT '发票图片物理路径',
  ocr_raw_json MEDIUMTEXT COMMENT 'OCR 原始返回 JSON',
  invoice_no VARCHAR(50) COMMENT '发票号码（OCR识别）',
  invoice_code VARCHAR(50) COMMENT '发票代码（OCR识别）',
  invoice_date DATETIME COMMENT '开票日期（OCR识别）',
  buyer_name VARCHAR(200) COMMENT '购买方名称（OCR识别）',
  seller_name VARCHAR(200) COMMENT '销售方名称（OCR识别）',
  buyer_tax_id VARCHAR(50) COMMENT '购买方税号（OCR识别）',
  seller_tax_id VARCHAR(50) COMMENT '销售方税号（OCR识别）',
  total_amount DECIMAL(10,2) COMMENT '价税合计金额（OCR识别）',
  invoice_type VARCHAR(50) COMMENT '发票类型（关联字典 reimbursement_invoice_type）',
  invoice_status VARCHAR(50) COMMENT '发票状态（关联字典 reimbursement_invoice_status）',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending=待确认, confirmed=已确认, discarded=已废弃',
  created_by BIGINT COMMENT '创建人ID',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_by BIGINT COMMENT '修改人ID',
  updated_at DATETIME NOT NULL COMMENT '修改时间',
  INDEX idx_reimbursement_id (reimbursement_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='临时发票表 - 每次OCR识别的结果';

-- 2. 发票类型字典（如果不存在）
INSERT IGNORE INTO sys_dict (id, dict_code, dict_name, description)
VALUES (7, 'reimbursement_invoice_type', '发票类型', '报销发票类型字典');

INSERT IGNORE INTO sys_dict_value (id, dict_id, label, value, sort_order, description)
VALUES
(26, 7, '增值税专用发票', 'special', 1, '增值税专用发票'),
(27, 7, '增值税普通发票', 'normal', 2, '增值税普通发票'),
(28, 7, '电子发票', 'electronic', 3, '电子发票'),
(29, 7, '定额发票', 'fixed', 4, '定额发票'),
(30, 7, '机动车发票', 'vehicle', 5, '机动车销售统一发票'),
(31, 7, '其他', 'other', 6, '其他类型发票');

-- 3. 发票状态字典（如果不存在）
INSERT IGNORE INTO sys_dict (id, dict_code, dict_name, description)
VALUES (8, 'reimbursement_invoice_status', '发票状态', '报销发票状态字典');

INSERT IGNORE INTO sys_dict_value (id, dict_id, label, value, sort_order, description)
VALUES
(32, 8, '正常', 'normal', 1, '发票正常'),
(33, 8, '已作废', 'voided', 2, '发票已作废'),
(34, 8, '已红冲', 'red_charged', 3, '发票已红冲');
