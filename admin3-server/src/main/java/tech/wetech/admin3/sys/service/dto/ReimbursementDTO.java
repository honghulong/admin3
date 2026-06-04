package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.InvoiceTemp;
import tech.wetech.admin3.sys.model.Reimbursement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReimbursementDTO(
  Long id,
  String title,
  String category,
  BigDecimal amount,
  String invoiceNo,
  String invoiceCode,
  LocalDateTime invoiceDate,
  String buyerName,
  String sellerName,
  String buyerTaxId,
  String sellerTaxId,
  String invoiceType,
  String invoiceStatus,
  String description,
  String status,
  Long applicantId,
  String applicantName,
  Long approverId,
  String approverName,
  String approveComment,
  LocalDateTime approveTime,
  Long createdBy,
  LocalDateTime createdAt,
  Long updatedBy,
  LocalDateTime updatedAt,
  List<AttachmentDTO> attachments,
  List<ApprovalLogDTO> approvalLogs,
  List<InvoiceTempDTO> invoiceTemps
) {

  public static ReimbursementDTO of(Reimbursement r, List<Attachment> attachments, List<ApprovalLog> approvalLogs, List<InvoiceTemp> invoiceTemps) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(),
      r.getInvoiceNo(), r.getInvoiceCode(), r.getInvoiceDate(), r.getBuyerName(),
      r.getSellerName(), r.getBuyerTaxId(), r.getSellerTaxId(),
      r.getInvoiceType(), r.getInvoiceStatus(),
      r.getDescription(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedBy(), r.getUpdatedAt(),
      attachments.stream().map(AttachmentDTO::new).toList(),
      approvalLogs.stream().map(ApprovalLogDTO::new).toList(),
      invoiceTemps.stream().map(InvoiceTempDTO::new).toList()
    );
  }

  public static ReimbursementDTO brief(Reimbursement r) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(),
      r.getInvoiceNo(), r.getInvoiceCode(), r.getInvoiceDate(), r.getBuyerName(),
      r.getSellerName(), r.getBuyerTaxId(), r.getSellerTaxId(),
      r.getInvoiceType(), r.getInvoiceStatus(),
      r.getDescription(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedBy(), r.getUpdatedAt(),
      null, null, null
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
    LocalDateTime createdAt,
    String ocrStatus,
    String ocrResult
  ) {
    public AttachmentDTO(Attachment a) {
      this(a.getId(), a.getReimbursementId(), a.getFileName(), a.getFileSize(),
        a.getFileType(), a.getFileUrl(), a.getUploadedBy(), a.getCreatedAt(),
        a.getOcrStatus(), a.getOcrResult());
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

  public record InvoiceTempDTO(
    Long id,
    Long reimbursementId,
    String imagePath,
    String ocrRawJson,
    String invoiceNo,
    String invoiceCode,
    LocalDateTime invoiceDate,
    String buyerName,
    String sellerName,
    String buyerTaxId,
    String sellerTaxId,
    BigDecimal totalAmount,
    String invoiceType,
    String invoiceStatus,
    String status,
    Long createdBy,
    LocalDateTime createdAt,
    Long updatedBy,
    LocalDateTime updatedAt
  ) {
    public InvoiceTempDTO(InvoiceTemp t) {
      this(t.getId(), t.getReimbursementId(), t.getImagePath(), t.getOcrRawJson(),
        t.getInvoiceNo(), t.getInvoiceCode(), t.getInvoiceDate(), t.getBuyerName(),
        t.getSellerName(), t.getBuyerTaxId(), t.getSellerTaxId(), t.getTotalAmount(),
        t.getInvoiceType(), t.getInvoiceStatus(), t.getStatus(),
        t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedBy(), t.getUpdatedAt());
    }
  }
}
