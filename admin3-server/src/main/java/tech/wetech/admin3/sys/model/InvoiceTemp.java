package tech.wetech.admin3.sys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 临时发票表 - 每次 OCR 识别的结果
 * 一个正式报销单（Reimbursement）可以对应多个临时发票记录
 */
@Entity
public class InvoiceTemp extends BaseEntity {

  /** 关联的正式报销单ID */
  private Long reimbursementId;

  /** 发票图片物理路径 */
  @Column(length = 500)
  private String imagePath;

  /** OCR 原始返回 JSON */
  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrRawJson;

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

  /** 价税合计金额（OCR识别） */
  @Column(precision = 10, scale = 2)
  private BigDecimal totalAmount;

  /** 发票类型（关联字典 reimbursement_invoice_type） */
  @Column(length = 50)
  private String invoiceType;

  /** 发票状态（关联字典 reimbursement_invoice_status） */
  @Column(length = 50)
  private String invoiceStatus;

  /** 状态: pending=待确认, confirmed=已确认, discarded=已废弃 */
  @Column(nullable = false, length = 20)
  private String status;

  /** 创建人ID */
  private Long createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** 修改人ID */
  private Long updatedBy;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public Long getReimbursementId() {
    return reimbursementId;
  }

  public void setReimbursementId(Long reimbursementId) {
    this.reimbursementId = reimbursementId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
