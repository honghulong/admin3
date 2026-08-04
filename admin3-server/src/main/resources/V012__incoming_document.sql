CREATE TABLE IF NOT EXISTS incoming_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL,
  carrier_no VARCHAR(100) NULL,
  title VARCHAR(255) NOT NULL,
  doc_no VARCHAR(100) NULL,
  secrecy_level VARCHAR(20) NULL,
  urgency_level VARCHAR(20) NULL,
  handler VARCHAR(100) NULL,
  remark TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(6) NOT NULL,
  updated_by BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_incoming_document_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来文登记';

CREATE TABLE IF NOT EXISTS incoming_scan_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  incoming_document_id BIGINT NOT NULL,
  attachment_id BIGINT NOT NULL,
  ocr_status VARCHAR(20) NULL,
  ocr_raw_json MEDIUMTEXT NULL,
  ocr_business_json MEDIUMTEXT NULL,
  created_at DATETIME(6) NOT NULL,
  created_by BIGINT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_incoming_scan_document_id (incoming_document_id),
  KEY idx_incoming_scan_attachment_id (attachment_id),
  KEY idx_incoming_scan_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来文扫描件附件';

CREATE TABLE IF NOT EXISTS ocr_mapping_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  module VARCHAR(80) NOT NULL,
  user_id BIGINT NULL,
  mapping_json MEDIUMTEXT NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ocr_mapping_module_user (module, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR字段映射配置';

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attachment' AND COLUMN_NAME = 'incoming_document_id'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE attachment ADD COLUMN incoming_document_id BIGINT NULL, ADD INDEX idx_attachment_incoming_document_id (incoming_document_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sys_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'sys' AND type = 0
  ORDER BY id
  LIMIT 1
);

SET @root_menu_id := 1;

SET @admin_role_id := (
  SELECT id FROM role
  WHERE name IN ('admin', '管理员', '超级管理员')
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT 'Document', '来文登记', NULL, 'incoming-document:view', 0, '/incoming-documents', @root_menu_id
WHERE @root_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'incoming-document:view');

UPDATE resource
SET parent_id = @root_menu_id
WHERE permission = 'incoming-document:view'
  AND type = 0
  AND parent_id <> @root_menu_id;

SET @incoming_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'incoming-document:view'
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, action_name, NULL, permission_value, 1, NULL, @incoming_menu_id
FROM (
  SELECT '新增来文' AS action_name, 'incoming-document:create' AS permission_value
  UNION ALL SELECT '编辑来文', 'incoming-document:update'
  UNION ALL SELECT '删除来文', 'incoming-document:delete'
) actions
WHERE @incoming_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = actions.permission_value);

INSERT INTO role_resource (role_id, resource_id)
SELECT @admin_role_id, r.id
FROM resource r
WHERE @admin_role_id IS NOT NULL
  AND r.permission IN ('incoming-document:view', 'incoming-document:create', 'incoming-document:update', 'incoming-document:delete')
  AND NOT EXISTS (
    SELECT 1 FROM role_resource rr WHERE rr.role_id = @admin_role_id AND rr.resource_id = r.id
  );
