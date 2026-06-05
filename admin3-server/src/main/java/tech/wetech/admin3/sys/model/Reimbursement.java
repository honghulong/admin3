package tech.wetech.admin3.sys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Reimbursement extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 20)
  private String category;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  /** 发票号码（OCR识别） */
  @Column(length = 50)
  private String invoiceNo;

  /** 发票代码（OCR识别） */
  @Column(length = 50)
  private String invoiceCode;

  /** 开票日期（OCR识别） */
  private LocalDateTime invoiceDate;

  /** 购买方名称（OCR识别） */
  @Column(length = 200)
  private String buyerName;

  /** 销售方名称（OCR识别） */
  @Column(length = 200)
  private String sellerName;

  /** 购买方税号（OCR识别） */
  @Column(length = 50)
  private String buyerTaxId;

  /** 销售方税号（OCR识别） */
  @Column(length = 50)
  private String sellerTaxId;

  /** 发票类型（关联字典 reimbursement_invoice_type） */
  @Column(length = 50)
  private String invoiceType;

  /** 发票状态（关联字典 reimbursement_invoice_status） */
  @Column(length = 50)
  private String invoiceStatus;

  @Column(columnDefinition = "TEXT")
  private String description;

  /** OCR 原始返回 JSON（最后一次有效识别的完整结果） */
  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrRawJson;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private Long applicantId;

  @Column(nullable = false, length = 50)
  private String applicantName;

  private Long approverId;

  @Column(length = 50)
  private String approverName;

  @Column(columnDefinition = "TEXT")
  private String approveComment;

  private LocalDateTime approveTime;

  /** 创建人ID（审计字段） */
  private Long createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** 修改人ID（审计字段） */
  private Long updatedBy;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public void setInvoiceNo(String invoiceNo) {
    this.invoiceNo = invoiceNo;
  }

  public String getInvoiceCode() {
    return invoiceCode;
  }

  public void setInvoiceCode(String invoiceCode) {
    this.invoiceCode = invoiceCode;
  }

  public LocalDateTime getInvoiceDate() {
    return invoiceDate;
  }

  public void setInvoiceDate(LocalDateTime invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public String getBuyerName() {
    return buyerName;
  }

  public void setBuyerName(String buyerName) {
    this.buyerName = buyerName;
  }

  public String getSellerName() {
    return sellerName;
  }

  public void setSellerName(String sellerName) {
    this.sellerName = sellerName;
  }

  public String getBuyerTaxId() {
    return buyerTaxId;
  }

  public void setBuyerTaxId(String buyerTaxId) {
    this.buyerTaxId = buyerTaxId;
  }

  public String getSellerTaxId() {
    return sellerTaxId;
  }

  public void setSellerTaxId(String sellerTaxId) {
    this.sellerTaxId = sellerTaxId;
  }

  public String getInvoiceType() {
    return invoiceType;
  }

  public void setInvoiceType(String invoiceType) {
    this.invoiceType = invoiceType;
  }

  public String getInvoiceStatus() {
    return invoiceStatus;
  }

  public void setInvoiceStatus(String invoiceStatus) {
    this.invoiceStatus = invoiceStatus;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOcrRawJson() {
    return ocrRawJson;
  }

  public void setOcrRawJson(String ocrRawJson) {
    this.ocrRawJson = ocrRawJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(Long applicantId) {
    this.applicantId = applicantId;
  }

  public String getApplicantName() {
    return applicantName;
  }

  public void setApplicantName(String applicantName) {
    this.applicantName = applicantName;
  }

  public Long getApproverId() {
    return approverId;
  }

  public void setApproverId(Long approverId) {
    this.approverId = approverId;
  }

  public String getApproverName() {
    return approverName;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  public String getApproveComment() {
    return approveComment;
  }

  public void setApproveComment(String approveComment) {
    this.approveComment = approveComment;
  }

  public LocalDateTime getApproveTime() {
    return approveTime;
  }

  public void setApproveTime(LocalDateTime approveTime) {
    this.approveTime = approveTime;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
