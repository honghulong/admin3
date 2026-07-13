CREATE TABLE IF NOT EXISTS reimbursement_expense_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  invoice_id BIGINT NULL,
  expense_date DATETIME NULL,
  start_place VARCHAR(100) NULL,
  end_place VARCHAR(100) NULL,
  kilometers DECIMAL(10,2) NULL,
  expense_type VARCHAR(50) NOT NULL,
  oa_expense_name VARCHAR(100) NULL,
  amount DECIMAL(10,2) NULL,
  project_cost_category VARCHAR(200) NULL,
  customer_name VARCHAR(200) NULL,
  remark TEXT NULL,
  description TEXT NULL,
  source VARCHAR(20) NULL,
  sort_order INT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_reimbursement_expense_item_reimbursement_id (reimbursement_id),
  KEY idx_reimbursement_expense_item_invoice_id (invoice_id)
);

ALTER TABLE invoice
  ADD COLUMN expense_item_id BIGINT NULL,
  ADD COLUMN seller_short_name VARCHAR(100) NULL,
  ADD COLUMN invoice_content_short_name VARCHAR(100) NULL,
  ADD COLUMN submit_date DATETIME NULL,
  ADD COLUMN duplicate_check_status VARCHAR(20) NULL,
  ADD COLUMN verify_status VARCHAR(20) NULL,
  ADD COLUMN ocr_confidence DECIMAL(5,2) NULL;
