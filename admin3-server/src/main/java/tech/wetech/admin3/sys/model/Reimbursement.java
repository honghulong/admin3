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

  @Column(nullable = false)
  private Integer invoiceCount = 0;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal invoiceTotalAmount = BigDecimal.ZERO;

  /** 鍙戠エ鍙风爜锛圤CR璇嗗埆锛?*/
  @Column(length = 50)
  private String invoiceNo;

  /** 鍙戠エ浠ｇ爜锛圤CR璇嗗埆锛?*/
  @Column(length = 50)
  private String invoiceCode;

  /** 寮€绁ㄦ棩鏈燂紙OCR璇嗗埆锛?*/
  private LocalDateTime invoiceDate;

  /** 璐拱鏂瑰悕绉帮紙OCR璇嗗埆锛?*/
  @Column(length = 200)
  private String buyerName;

  /** 閿€鍞柟鍚嶇О锛圤CR璇嗗埆锛?*/
  @Column(length = 200)
  private String sellerName;

  /** 璐拱鏂圭◣鍙凤紙OCR璇嗗埆锛?*/
  @Column(length = 50)
  private String buyerTaxId;

  /** 閿€鍞柟绋庡彿锛圤CR璇嗗埆锛?*/
  @Column(length = 50)
  private String sellerTaxId;

  /** 鍙戠エ绫诲瀷锛堝叧鑱斿瓧鍏?reimbursement_invoice_type锛?*/
  @Column(length = 50)
  private String invoiceType;

  /** 鍙戠エ鐘舵€侊紙鍏宠仈瀛楀吀 reimbursement_invoice_status锛?*/
  @Column(length = 50)
  private String invoiceStatus;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(columnDefinition = "TEXT")
  private String matterDescription;

  /** OCR 鍘熷杩斿洖 JSON锛堟渶鍚庝竴娆℃湁鏁堣瘑鍒殑瀹屾暣缁撴灉锛?*/
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

  /** 鍒涘缓浜篒D锛堝璁″瓧娈碉級 */
  private Long createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** 淇敼浜篒D锛堝璁″瓧娈碉級 */
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

  public Integer getInvoiceCount() {
    return invoiceCount;
  }

  public void setInvoiceCount(Integer invoiceCount) {
    this.invoiceCount = invoiceCount;
  }

  public BigDecimal getInvoiceTotalAmount() {
    return invoiceTotalAmount;
  }

  public void setInvoiceTotalAmount(BigDecimal invoiceTotalAmount) {
    this.invoiceTotalAmount = invoiceTotalAmount;
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

  public String getMatterDescription() {
    return matterDescription;
  }

  public void setMatterDescription(String matterDescription) {
    this.matterDescription = matterDescription;
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

