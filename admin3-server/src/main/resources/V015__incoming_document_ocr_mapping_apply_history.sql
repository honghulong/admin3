SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incoming_document' AND COLUMN_NAME = 'ocr_mapping_apply_history'
);
SET @sql := IF(@column_exists = 0, 'ALTER TABLE incoming_document ADD COLUMN ocr_mapping_apply_history MEDIUMTEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
