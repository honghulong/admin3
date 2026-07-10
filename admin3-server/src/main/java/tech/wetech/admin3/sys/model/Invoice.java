package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Invoice extends BaseEntity {

  @Column(nullable = false)
  private Long reimbursementId;

  private Long invoiceTempId;

  private Long attachmentId;

  @Column(length = 500)
  private String imagePath;

  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrRawJson;

  @Column(length = 50)
  private String invoiceNo;

  @Column(length = 50)
  private String invoiceCode;

  private LocalDateTime invoiceDate;

  @Column(length = 200)
  private String buyerName;

  @Column(length = 200)
  private String sellerName;

  @Column(length = 50)
  private String buyerTaxId;

  @Column(length = 50)
  private String sellerTaxId;

  @Column(precision = 10, scale = 2)
  private BigDecimal totalAmount;

  @Column(length = 50)
  private String invoiceType;

  @Column(length = 50)
  private String invoiceStatus;

  @Column(precision = 10, scale = 2)
  private BigDecimal amountBeforeTax;

  @Column(length = 20)
  private String taxRate;

  @Column(precision = 10, scale = 2)
  private BigDecimal taxAmount;

  @Column(length = 100)
  private String payee;

  @Column(length = 100)
  private String reviewer;

  @Column(length = 100)
  private String issuer;

  @Column(length = 500)
  private String buyerAddressPhone;

  @Column(length = 500)
  private String buyerBankInfo;

  @Column(length = 500)
  private String sellerAddressPhone;

  @Column(length = 500)
  private String sellerBankInfo;

  @Column(columnDefinition = "TEXT")
  private String remarks;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private Boolean deleted = false;

  private LocalDateTime deletedAt;

  private Long createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private Long updatedBy;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public Long getReimbursementId() {
    return reimbursementId;
  }

  public void setReimbursementId(Long reimbursementId) {
    this.reimbursementId = reimbursementId;
  }

  public Long getInvoiceTempId() {
    return invoiceTempId;
  }

  public void setInvoiceTempId(Long invoiceTempId) {
    this.invoiceTempId = invoiceTempId;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public void setAttachmentId(Long attachmentId) {
    this.attachmentId = attachmentId;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public String getOcrRawJson() {
    return ocrRawJson;
  }

  public void setOcrRawJson(String ocrRawJson) {
    this.ocrRawJson = ocrRawJson;
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

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
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

  public BigDecimal getAmountBeforeTax() {
    return amountBeforeTax;
  }

  public void setAmountBeforeTax(BigDecimal amountBeforeTax) {
    this.amountBeforeTax = amountBeforeTax;
  }

  public String getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(String taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(BigDecimal taxAmount) {
    this.taxAmount = taxAmount;
  }

  public String getPayee() {
    return payee;
  }

  public void setPayee(String payee) {
    this.payee = payee;
  }

  public String getReviewer() {
    return reviewer;
  }

  public void setReviewer(String reviewer) {
    this.reviewer = reviewer;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getBuyerAddressPhone() {
    return buyerAddressPhone;
  }

  public void setBuyerAddressPhone(String buyerAddressPhone) {
    this.buyerAddressPhone = buyerAddressPhone;
  }

  public String getBuyerBankInfo() {
    return buyerBankInfo;
  }

  public void setBuyerBankInfo(String buyerBankInfo) {
    this.buyerBankInfo = buyerBankInfo;
  }

  public String getSellerAddressPhone() {
    return sellerAddressPhone;
  }

  public void setSellerAddressPhone(String sellerAddressPhone) {
    this.sellerAddressPhone = sellerAddressPhone;
  }

  public String getSellerBankInfo() {
    return sellerBankInfo;
  }

  public void setSellerBankInfo(String sellerBankInfo) {
    this.sellerBankInfo = sellerBankInfo;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
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

