-- Add logical delete metadata for uploaded reimbursement files.

ALTER TABLE attachment
  ADD COLUMN deleted BIT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  ADD COLUMN deleted_at DATETIME NULL COMMENT 'Logical delete time',
  ADD INDEX idx_attachment_deleted (deleted);
