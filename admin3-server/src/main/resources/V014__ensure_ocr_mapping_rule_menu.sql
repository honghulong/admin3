SET @mapping_rule_menu_name := CONVERT(UNHEX('e698a0e5b084e8a784e58899e7aea1e79086') USING utf8mb4);
SET @mapping_rule_update_name := CONVERT(UNHEX('e7bc96e8be91e698a0e5b084e8a784e58899') USING utf8mb4);
SET @mapping_rule_delete_name := CONVERT(UNHEX('e588a0e999a4e698a0e5b084e8a784e58899') USING utf8mb4);

SET @root_menu_id := 1;

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT 'Connection', @mapping_rule_menu_name, NULL, 'ocr-mapping-rule:view', 0, '/ocr-mapping-rules', @root_menu_id
WHERE @root_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = 'ocr-mapping-rule:view');

UPDATE resource
SET icon = 'Connection',
    name = @mapping_rule_menu_name,
    url = '/ocr-mapping-rules',
    parent_id = @root_menu_id
WHERE permission = 'ocr-mapping-rule:view'
  AND type = 0;

SET @rule_menu_id := (
  SELECT id FROM resource
  WHERE permission = 'ocr-mapping-rule:view' AND type = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id)
SELECT NULL, action_name, NULL, permission_value, 1, NULL, @rule_menu_id
FROM (
  SELECT @mapping_rule_update_name AS action_name, 'ocr-mapping-rule:update' AS permission_value
  UNION ALL SELECT @mapping_rule_delete_name, 'ocr-mapping-rule:delete'
) actions
WHERE @rule_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM resource WHERE permission = actions.permission_value);

UPDATE resource
SET parent_id = @rule_menu_id
WHERE permission IN ('ocr-mapping-rule:update', 'ocr-mapping-rule:delete')
  AND type = 1;

SET @admin_role_id := (
  SELECT id FROM role
  WHERE name = 'admin'
  ORDER BY id
  LIMIT 1
);

INSERT INTO role_resource (role_id, resource_id)
SELECT @admin_role_id, r.id
FROM resource r
WHERE @admin_role_id IS NOT NULL
  AND r.permission IN ('ocr-mapping-rule:view', 'ocr-mapping-rule:update', 'ocr-mapping-rule:delete')
  AND NOT EXISTS (
    SELECT 1 FROM role_resource rr WHERE rr.role_id = @admin_role_id AND rr.resource_id = r.id
  );
