-- Reimbursement invoice parent-child restructuring.
-- This project currently relies on JPA ddl-auto=update, so keep this file as
-- the executable/manual SQL record for environments that manage schema explicitly.

ALTER TABLE reimbursement
  ADD COLUMN invoice_count INT NOT NULL DEFAULT 0 COMMENT 'Active invoice count',
  ADD COLUMN invoice_total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Active invoice total amount';

CREATE TABLE IF NOT EXISTS invoice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
  reimbursement_id BIGINT NOT NULL COMMENT 'Parent reimbursement id',
  invoice_temp_id BIGINT NULL COMMENT 'Source temporary invoice id',
  attachment_id BIGINT NULL COMMENT 'Source attachment id',
  image_path VARCHAR(500) NULL COMMENT 'Invoice image path or URL',
  ocr_raw_json MEDIUMTEXT NULL COMMENT 'OCR raw JSON',
  invoice_no VARCHAR(50) NULL COMMENT 'Invoice number',
  invoice_code VARCHAR(50) NULL COMMENT 'Invoice code',
  invoice_date DATETIME NULL COMMENT 'Invoice date',
  buyer_name VARCHAR(200) NULL COMMENT 'Buyer name',
  seller_name VARCHAR(200) NULL COMMENT 'Seller name',
  buyer_tax_id VARCHAR(50) NULL COMMENT 'Buyer tax id',
  seller_tax_id VARCHAR(50) NULL COMMENT 'Seller tax id',
  total_amount DECIMAL(10,2) NULL COMMENT 'Invoice total amount',
  invoice_type VARCHAR(50) NULL COMMENT 'Invoice type',
  invoice_status VARCHAR(50) NULL COMMENT 'Invoice status',
  status VARCHAR(20) NOT NULL DEFAULT 'confirmed' COMMENT 'Business status',
  deleted BIT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  deleted_at DATETIME NULL COMMENT 'Logical delete time',
  created_by BIGINT NULL COMMENT 'Creator id',
  created_at DATETIME NOT NULL COMMENT 'Created time',
  updated_by BIGINT NULL COMMENT 'Updater id',
  updated_at DATETIME NOT NULL COMMENT 'Updated time',
  INDEX idx_invoice_reimbursement_id (reimbursement_id),
  INDEX idx_invoice_attachment_id (attachment_id),
  INDEX idx_invoice_temp_id (invoice_temp_id),
  INDEX idx_invoice_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Invoice child table';

ALTER TABLE invoice_temp
  ADD COLUMN invoice_id BIGINT NULL COMMENT 'Linked formal invoice id',
  ADD COLUMN attachment_id BIGINT NULL COMMENT 'Source attachment id',
  ADD INDEX idx_invoice_temp_invoice_id (invoice_id),
  ADD INDEX idx_invoice_temp_attachment_id (attachment_id);

INSERT INTO invoice (
  reimbursement_id, ocr_raw_json, invoice_no, invoice_code, invoice_date,
  buyer_name, seller_name, buyer_tax_id, seller_tax_id, total_amount,
  invoice_type, invoice_status, status, deleted, created_by, created_at, updated_by, updated_at
)
SELECT
  id, ocr_raw_json, invoice_no, invoice_code, invoice_date,
  buyer_name, seller_name, buyer_tax_id, seller_tax_id, amount,
  invoice_type, invoice_status, 'confirmed', 0, created_by, created_at, updated_by, updated_at
FROM reimbursement
WHERE invoice_no IS NOT NULL
   OR invoice_code IS NOT NULL
   OR ocr_raw_json IS NOT NULL;

UPDATE reimbursement r
LEFT JOIN (
  SELECT reimbursement_id, COUNT(*) AS invoice_count, COALESCE(SUM(total_amount), 0) AS invoice_total_amount
  FROM invoice
  WHERE deleted = 0
  GROUP BY reimbursement_id
) s ON s.reimbursement_id = r.id
SET r.invoice_count = COALESCE(s.invoice_count, 0),
    r.invoice_total_amount = COALESCE(s.invoice_total_amount, 0);
