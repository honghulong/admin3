-- 标记交通大类费用类型：交通类发票默认一票一费用明细，便于单独关联行程单/凭证。

SET @expense_type_dict_id := (
  SELECT id FROM sys_dict WHERE dict_code = 'reimbursement_expense_type' LIMIT 1
);

UPDATE sys_dict_value
SET description = CASE
  WHEN description IS NULL OR description = '' THEN 'transport'
  WHEN description NOT LIKE '%transport%' THEN CONCAT(description, ';transport')
  ELSE description
END
WHERE dict_id = @expense_type_dict_id
  AND value IN (
    'toll',
    'rail',
    'metro',
    'ride_hailing',
    'carpool',
    'freight',
    'air_travel',
    'taxi',
    'bus',
    'rental_car',
    'parking'
  );
