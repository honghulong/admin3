SET @ocr_engine_parse_strategy_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ocr_engine_config'
    AND column_name = 'parse_strategy'
);

SET @ocr_engine_parse_strategy_sql := IF(
  @ocr_engine_parse_strategy_exists = 0,
  'ALTER TABLE ocr_engine_config ADD COLUMN parse_strategy VARCHAR(30) NOT NULL DEFAULT ''BBOX_REGION''',
  'SELECT 1'
);

PREPARE ocr_engine_parse_strategy_stmt FROM @ocr_engine_parse_strategy_sql;
EXECUTE ocr_engine_parse_strategy_stmt;
DEALLOCATE PREPARE ocr_engine_parse_strategy_stmt;

UPDATE ocr_engine_config
SET parse_strategy = 'BBOX_REGION'
WHERE parse_strategy IS NULL OR parse_strategy = '';
