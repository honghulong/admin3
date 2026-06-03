package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.Reimbursement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReimbursementDTO(
  Long id,
  String title,
  String category,
  BigDecimal amount,
  String description,
  String status,
  Long applicantId,
  String applicantName,
  Long approverId,
  String approverName,
  String approveComment,
  LocalDateTime approveTime,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  List<AttachmentDTO> attachments,
  List<ApprovalLogDTO> approvalLogs
) {

  public static ReimbursementDTO of(Reimbursement r, List<Attachment> attachments, List<ApprovalLog> approvalLogs) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(),
      r.getDescription(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedAt(), r.getUpdatedAt(),
      attachments.stream().map(AttachmentDTO::new).toList(),
      approvalLogs.stream().map(ApprovalLogDTO::new).toList()
    );
  }

  public static ReimbursementDTO brief(Reimbursement r) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(),
      r.getDescription(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedAt(), r.getUpdatedAt(),
      null, null
    );
  }

  public record AttachmentDTO(
    Long id,
    Long reimbursementId,
    String fileName,
    Long fileSize,
    String fileType,
    String fileUrl,
    Long uploadedBy,
    LocalDateTime createdAt
  ) {
    public AttachmentDTO(Attachment a) {
      this(a.getId(), a.getReimbursementId(), a.getFileName(), a.getFileSize(),
        a.getFileType(), a.getFileUrl(), a.getUploadedBy(), a.getCreatedAt());
    }
  }

  public record ApprovalLogDTO(
    Long id,
    Long reimbursementId,
    String action,
    Long operatorId,
    String operatorName,
    String comment,
    LocalDateTime createdAt
  ) {
    public ApprovalLogDTO(ApprovalLog l) {
      this(l.getId(), l.getReimbursementId(), l.getAction(), l.getOperatorId(),
        l.getOperatorName(), l.getComment(), l.getCreatedAt());
    }
  }
}
