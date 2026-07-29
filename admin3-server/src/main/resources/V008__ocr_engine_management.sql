CREATE TABLE IF NOT EXISTS ocr_engine_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  provider_type VARCHAR(30) NOT NULL,
  base_url VARCHAR(500) NULL,
  model_name VARCHAR(100) NULL,
  api_key_cipher_text TEXT NULL,
  protocol VARCHAR(30) NULL,
  tenant_id VARCHAR(100) NULL,
  environment VARCHAR(50) NULL,
  prompt TEXT NULL,
  parse_strategy VARCHAR(30) NOT NULL DEFAULT 'BBOX_REGION',
  enabled BIT(1) NOT NULL DEFAULT b'1',
  is_default BIT(1) NOT NULL DEFAULT b'0',
  fallback_enabled BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ocr_engine_enabled_default (enabled, is_default),
  KEY idx_ocr_engine_provider (provider_type, enabled)
);

SET @invoice_ocr_prompt := CONVERT(UNHEX('e8afb7e8af86e588abe59bbee78987e4b8ade79a84e4b8ade59bbde58f91e7a5a8efbc8ce5b9b6e58faae8bf94e59b9ee4b880e4b8aa204a534f4e20e5afb9e8b1a1efbc8ce4b88de8a681e8bf94e59b9e204d61726b646f776e20e4bba3e7a081e59d97e380820ae5ad97e6aeb5e5908de5bf85e9a1bbe4bdbfe794a8e7b3bbe7bb9fe6a087e5878620736e616b655f6361736520e5ad97e6aeb5efbc8ce5ad97e6aeb5e887b3e5b091e58c85e68bacefbc9a0a696e766f6963655f747970652c20696e766f6963655f6e6f2c20696e766f6963655f636f64652c20646174652c2062757965725f6e616d652c2062757965725f7461785f69642c2062757965725f616464726573735f70686f6e652c2062757965725f62616e6b5f696e666f2c0a73656c6c65725f6e616d652c2073656c6c65725f7461785f69642c2073656c6c65725f616464726573735f70686f6e652c2073656c6c65725f62616e6b5f696e666f2c20746f74616c5f616d6f756e742c20616d6f756e745f6265666f72655f7461782c207461785f616d6f756e742c0a7461785f726174652c20636865636b5f636f64652c206d616368696e655f6e6f2c2070617965652c2072657669657765722c206973737565722c2072656d61726b732c2073746172745f706c6163652c20656e645f706c616365e380820ae5ad97e6aeb5e698a0e5b084e8a784e58899efbc9a0a2d20e58f91e7a5a8e58fb7e7a081202d3e20696e766f6963655f6e6fefbc9be58f91e7a5a8e4bba3e7a081202d3e20696e766f6963655f636f6465efbc9be5bc80e7a5a8e697a5e69c9f202d3e2064617465efbc9be69cbae599a8e7bc96e58fb7202d3e206d616368696e655f6e6fefbc9be6a0a1e9aa8ce7a081202d3e20636865636b5f636f6465e380820a2d20e8b4ade4b9b0e696b9e5908de7a7b0202d3e2062757965725f6e616d65efbc9be8b4ade4b9b0e696b9e7bab3e7a88ee4babae8af86e588abe58fb72fe7bb9fe4b880e7a4bee4bc9ae4bfa1e794a8e4bba3e7a081202d3e2062757965725f7461785f6964e380820a2d20e8b4ade4b9b0e696b9e59cb0e59d80e38081e794b5e8af9d202d3e2062757965725f616464726573735f70686f6e65e38082e58db3e4bdbfe59cb0e59d80e5928ce794b5e8af9de59ca8e5908ce4b880e8a18cefbc8ce4b99fe8a681e5ae8ce695b4e68f90e58f96efbc8ce4b88de8a681e694bee5bc83e380820a2d20e8b4ade4b9b0e696b9e5bc80e688b7e8a18ce58f8ae8b4a6e58fb7202d3e2062757965725f62616e6b5f696e666fe38082e993b6e8a18ce5908de7a7b0e5928ce8b4a6e58fb7e8a681e5b0bde9878fe4b880e8b5b7e4bf9de79599e380820a2d20e99480e594aee696b9e5908de7a7b0202d3e2073656c6c65725f6e616d65efbc9be99480e594aee696b9e7bab3e7a88ee4babae8af86e588abe58fb72fe7bb9fe4b880e7a4bee4bc9ae4bfa1e794a8e4bba3e7a081202d3e2073656c6c65725f7461785f6964e380820a2d20e99480e594aee696b9e59cb0e59d80e38081e794b5e8af9d202d3e2073656c6c65725f616464726573735f70686f6e65e38082e58db3e4bdbfe59cb0e59d80e5928ce794b5e8af9de59ca8e5908ce4b880e8a18cefbc8ce4b99fe8a681e5ae8ce695b4e68f90e58f96efbc8ce4b88de8a681e694bee5bc83e380820a2d20e99480e594aee696b9e5bc80e688b7e8a18ce58f8ae8b4a6e58fb7202d3e2073656c6c65725f62616e6b5f696e666fe38082e993b6e8a18ce5908de7a7b0e5928ce8b4a6e58fb7e8a681e5b0bde9878fe4b880e8b5b7e4bf9de79599e380820a2d20e4bbb7e7a88ee59088e8aea1e5b08fe58699e98791e9a29d202d3e20746f74616c5f616d6f756e74efbc9be59088e8aea1e4b88de590abe7a88ee98791e9a29d202d3e20616d6f756e745f6265666f72655f746178efbc9be7a88ee9a29d202d3e207461785f616d6f756e74efbc9be7a88ee78e87202d3e207461785f72617465e380820a2d20e694b6e6acbee4baba202d3e207061796565efbc9be5a48de6a0b8e4baba202d3e207265766965776572efbc9be5bc80e7a5a8e4baba202d3e20697373756572efbc9be5a487e6b3a8202d3e2072656d61726b73e380820ae8a681e6b182efbc9a0a2d20e58faae8bf94e59b9e204a534f4e20e5afb9e8b1a1e380820a2d20e883bde79c8be6b885e4bd86e4b88de7a1aee5ae9ae79a84e5ad97e6aeb5e4b99fe8a681e5b0bde9878fe8bf94e59b9ee58e9fe69687efbc8ce4b88de8a681e8bdbbe69893e8bf94e59b9ee7a9bae380820a2d20e697a0e6b395e8af86e588abe79a84e5ad97e6aeb5e8bf94e59b9ee7a9bae5ad97e7aca6e4b8b2efbc8ce4b88de8a681e8bf94e59b9e206e756c6c20e6889620226e756c6c22e38082') USING utf8mb4);

INSERT INTO ocr_engine_config (
  name, provider_type, base_url, model_name, protocol, enabled, is_default,
  fallback_enabled, created_at, updated_at
)
SELECT 'Internal OCR', 'INTERNAL', 'http://localhost:8765', NULL, 'INTERNAL_HTTP',
       b'1', b'1', b'0', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ocr_engine_config WHERE provider_type = 'INTERNAL'
);

INSERT INTO ocr_engine_config (
  name, provider_type, base_url, model_name, protocol, prompt, enabled, is_default,
  fallback_enabled, created_at, updated_at
)
SELECT 'Qwen OCR', 'QWEN', 'https://llm-4t53i4opya4nag5m.cn-beijing.maas.aliyuncs.com/compatible-mode/v1', 'qwen3.5-ocr',
       'OPENAI_COMPATIBLE', @invoice_ocr_prompt,
       b'0', b'0', b'1', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ocr_engine_config WHERE provider_type = 'QWEN'
);

INSERT INTO ocr_engine_config (
  name, provider_type, base_url, model_name, protocol, prompt, enabled, is_default,
  fallback_enabled, created_at, updated_at
)
SELECT 'Kimi Vision OCR', 'KIMI', 'https://api.moonshot.cn/v1', 'kimi-k2.6',
       'OPENAI_COMPATIBLE', @invoice_ocr_prompt,
       b'0', b'0', b'1', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ocr_engine_config WHERE provider_type = 'KIMI'
);

SET @sys_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'sys' AND type = 0
  ORDER BY id
  LIMIT 1
);

SET @admin_role_id := (
  SELECT id FROM role
  WHERE name IN ('admin', '管理员', '超级管理员')
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT 'Operation', 'OCR 执行管理', NULL, 'ocr-engine:view', 0, '/ocr-engine-configs', @sys_menu_id
WHERE @sys_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-engine:view');

SET @ocr_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'ocr-engine:view' AND type = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, '新增 OCR 引擎配置', NULL, 'ocr-engine:create', 1, NULL, @ocr_menu_id
WHERE @ocr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-engine:create');

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, '修改 OCR 引擎配置', NULL, 'ocr-engine:update', 1, NULL, @ocr_menu_id
WHERE @ocr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-engine:update');

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, '删除 OCR 引擎配置', NULL, 'ocr-engine:delete', 1, NULL, @ocr_menu_id
WHERE @ocr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-engine:delete');

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, '使用此 OCR 引擎', NULL, 'ocr-engine:markAsDefault', 1, NULL, @ocr_menu_id
WHERE @ocr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-engine:markAsDefault');

INSERT INTO role_resource (role_id, resource_id)
SELECT @admin_role_id, r.id
FROM resource r
WHERE @admin_role_id IS NOT NULL
  AND r.permission IN (
    'ocr-engine:view',
    'ocr-engine:create',
    'ocr-engine:update',
    'ocr-engine:delete',
    'ocr-engine:markAsDefault'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_resource rr
    WHERE rr.role_id = @admin_role_id AND rr.resource_id = r.id
  );
