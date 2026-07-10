-- Add employee-entered detailed reimbursement matter description.

ALTER TABLE reimbursement
  ADD COLUMN matter_description TEXT NULL COMMENT '报销事项详细描述';
