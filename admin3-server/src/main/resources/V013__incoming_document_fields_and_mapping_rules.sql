SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'incoming_unit'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN incoming_unit VARCHAR(200) NULL AFTER carrier_no', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'received_no'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN received_no VARCHAR(100) NULL AFTER doc_no', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'received_date'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN received_date VARCHAR(50) NULL AFTER received_no', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'office_opinion'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN office_opinion TEXT NULL AFTER handler', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'leader_instruction'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN leader_instruction TEXT NULL AFTER office_opinion', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'reader_signer'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN reader_signer VARCHAR(100) NULL AFTER leader_instruction', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'handling_status'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN handling_status TEXT NULL AFTER reader_signer', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN deleted BIT(1) NOT NULL DEFAULT b''0'' AFTER updated_by, ADD COLUMN deleted_at DATETIME(6) NULL AFTER deleted, ADD INDEX idx_incoming_document_deleted (deleted)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ocr_mapping_rule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  module VARCHAR(80) NOT NULL,
  form_type VARCHAR(80) NULL,
  mapping_json MEDIUMTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(6) NOT NULL,
  updated_by BIGINT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ocr_mapping_rule_name (name),
  KEY idx_ocr_mapping_rule_module (module),
  KEY idx_ocr_mapping_rule_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR映射规则';

SET @root_menu_id := 1;

SET @admin_role_id := (
  SELECT id FROM role
  WHERE name IN ('admin', '管理员', '超级管理员')
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT 'Connection', '映射规则管理', NULL, 'ocr-mapping-rule:view', 0, '/ocr-mapping-rules', @root_menu_id
WHERE @root_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-mapping-rule:view');

SET @rule_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'ocr-mapping-rule:view'
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, action_name, NULL, permission_value, 1, NULL, @rule_menu_id
FROM (
  SELECT '编辑映射规则' AS action_name, 'ocr-mapping-rule:update' AS permission_value
  UNION ALL SELECT '删除映射规则', 'ocr-mapping-rule:delete'
) actions
WHERE @rule_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = actions.permission_value);

INSERT INTO role_resource (role_id, resource_id)
SELECT @admin_role_id, r.id
FROM resource r
WHERE @admin_role_id IS NOT NULL
  AND r.permission IN ('ocr-mapping-rule:view', 'ocr-mapping-rule:update', 'ocr-mapping-rule:delete')
  AND NOT EXISTS (
    SELECT 1 FROM role_resource rr WHERE rr.role_id = @admin_role_id AND rr.resource_id = r.id
  );
