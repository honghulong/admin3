package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.InvoiceTemp;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReimbursementDTO(
  Long id,
  String title,
  String category,
  BigDecimal amount,
  Integer invoiceCount,
  BigDecimal invoiceTotalAmount,
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
  String matterDescription,
  String ocrRawJson,
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
  List<InvoiceDTO> invoices,
  List<ExpenseItemDTO> expenseItems,
  List<InvoiceTempDTO> invoiceTemps
) {

  public static ReimbursementDTO of(Reimbursement r, List<Attachment> attachments, List<ApprovalLog> approvalLogs,
                                    List<Invoice> invoices, List<ReimbursementExpenseItem> expenseItems,
                                    List<InvoiceTemp> invoiceTemps) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(), r.getInvoiceCount(), r.getInvoiceTotalAmount(),
      r.getInvoiceNo(), r.getInvoiceCode(), r.getInvoiceDate(), r.getBuyerName(),
      r.getSellerName(), r.getBuyerTaxId(), r.getSellerTaxId(),
      r.getInvoiceType(), r.getInvoiceStatus(),
      r.getDescription(), r.getMatterDescription(), r.getOcrRawJson(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedBy(), r.getUpdatedAt(),
      attachments.stream().map(AttachmentDTO::new).toList(),
      approvalLogs.stream().map(ApprovalLogDTO::new).toList(),
      invoices.stream().map(InvoiceDTO::new).toList(),
      expenseItems.stream().map(item -> new ExpenseItemDTO(item, invoices)).toList(),
      invoiceTemps.stream().map(InvoiceTempDTO::new).toList()
    );
  }

  public static ReimbursementDTO brief(Reimbursement r) {
    return new ReimbursementDTO(
      r.getId(), r.getTitle(), r.getCategory(), r.getAmount(), r.getInvoiceCount(), r.getInvoiceTotalAmount(),
      r.getInvoiceNo(), r.getInvoiceCode(), r.getInvoiceDate(), r.getBuyerName(),
      r.getSellerName(), r.getBuyerTaxId(), r.getSellerTaxId(),
      r.getInvoiceType(), r.getInvoiceStatus(),
      r.getDescription(), r.getMatterDescription(), r.getOcrRawJson(), r.getStatus(), r.getApplicantId(), r.getApplicantName(),
      r.getApproverId(), r.getApproverName(), r.getApproveComment(), r.getApproveTime(),
      r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedBy(), r.getUpdatedAt(),
      null, null, null, null, null
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

  public record InvoiceDTO(
    Long id,
    Long reimbursementId,
    Long invoiceTempId,
    Long attachmentId,
    Long expenseItemId,
    String imagePath,
    String ocrRawJson,
    String invoiceNo,
    String invoiceCode,
    LocalDateTime invoiceDate,
    String buyerName,
    String sellerName,
    String sellerShortName,
    String invoiceContentShortName,
    LocalDateTime submitDate,
    String buyerTaxId,
    String sellerTaxId,
    BigDecimal totalAmount,
    String invoiceType,
    String invoiceStatus,
    String duplicateCheckStatus,
    String verifyStatus,
    BigDecimal ocrConfidence,
    BigDecimal amountBeforeTax,
    String taxRate,
    BigDecimal taxAmount,
    String payee,
    String reviewer,
    String issuer,
    String buyerAddressPhone,
    String buyerBankInfo,
    String sellerAddressPhone,
    String sellerBankInfo,
    String remarks,
    String status,
    Long createdBy,
    LocalDateTime createdAt,
    Long updatedBy,
    LocalDateTime updatedAt
  ) {
    public InvoiceDTO(Invoice i) {
      this(i.getId(), i.getReimbursementId(), i.getInvoiceTempId(), i.getAttachmentId(), i.getExpenseItemId(),
        i.getImagePath(), i.getOcrRawJson(), i.getInvoiceNo(), i.getInvoiceCode(),
        i.getInvoiceDate(), i.getBuyerName(), i.getSellerName(), i.getSellerShortName(),
        i.getInvoiceContentShortName(), i.getSubmitDate(), i.getBuyerTaxId(),
        i.getSellerTaxId(), i.getTotalAmount(), i.getInvoiceType(), i.getInvoiceStatus(),
        i.getDuplicateCheckStatus(), i.getVerifyStatus(), i.getOcrConfidence(),
        i.getAmountBeforeTax(), i.getTaxRate(), i.getTaxAmount(), i.getPayee(),
        i.getReviewer(), i.getIssuer(), i.getBuyerAddressPhone(), i.getBuyerBankInfo(),
        i.getSellerAddressPhone(), i.getSellerBankInfo(), i.getRemarks(), i.getStatus(),
        i.getCreatedBy(), i.getCreatedAt(), i.getUpdatedBy(), i.getUpdatedAt());
    }
  }

  public record ExpenseItemDTO(
    Long id,
    Long reimbursementId,
    Long invoiceId,
    LocalDateTime expenseDate,
    String startPlace,
    String endPlace,
    BigDecimal kilometers,
    String expenseType,
    String oaExpenseName,
    BigDecimal amount,
    String projectCostCategory,
    String customerName,
    String remark,
    String description,
    String source,
    List<Long> invoiceIds,
    Integer invoiceCount,
    BigDecimal invoiceTotalAmount,
    Integer sortOrder,
    Long createdBy,
    LocalDateTime createdAt,
    Long updatedBy,
    LocalDateTime updatedAt
  ) {
    public ExpenseItemDTO(ReimbursementExpenseItem item, List<Invoice> invoices) {
      this(item.getId(), item.getReimbursementId(), item.getInvoiceId(), item.getExpenseDate(),
        item.getStartPlace(), item.getEndPlace(), item.getKilometers(), item.getExpenseType(),
        item.getOaExpenseName(), item.getAmount(), item.getProjectCostCategory(), item.getCustomerName(),
        item.getRemark(), item.getDescription(), item.getSource(), invoiceIds(item, invoices),
        invoiceCount(item, invoices), invoiceTotalAmount(item, invoices), item.getSortOrder(),
        item.getCreatedBy(), item.getCreatedAt(), item.getUpdatedBy(), item.getUpdatedAt());
    }

    private static List<Long> invoiceIds(ReimbursementExpenseItem item, List<Invoice> invoices) {
      return invoices.stream()
        .filter(invoice -> item.getId() != null && item.getId().equals(invoice.getExpenseItemId()))
        .map(Invoice::getId)
        .toList();
    }

    private static Integer invoiceCount(ReimbursementExpenseItem item, List<Invoice> invoices) {
      return invoiceIds(item, invoices).size();
    }

    private static BigDecimal invoiceTotalAmount(ReimbursementExpenseItem item, List<Invoice> invoices) {
      return invoices.stream()
        .filter(invoice -> item.getId() != null && item.getId().equals(invoice.getExpenseItemId()))
        .map(Invoice::getTotalAmount)
        .filter(amount -> amount != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
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
