package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class IncomingDocument extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(length = 100)
  private String carrierNo;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(length = 100)
  private String docNo;

  @Column(length = 20)
  private String secrecyLevel;

  @Column(length = 20)
  private String urgencyLevel;

  @Column(length = 100)
  private String handler;

  @Column(columnDefinition = "TEXT")
  private String remark;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private Long createdBy;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column
  private Long updatedBy;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getCarrierNo() {
    return carrierNo;
  }

  public void setCarrierNo(String carrierNo) {
    this.carrierNo = carrierNo;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDocNo() {
    return docNo;
  }

  public void setDocNo(String docNo) {
    this.docNo = docNo;
  }

  public String getSecrecyLevel() {
    return secrecyLevel;
  }

  public void setSecrecyLevel(String secrecyLevel) {
    this.secrecyLevel = secrecyLevel;
  }

  public String getUrgencyLevel() {
    return urgencyLevel;
  }

  public void setUrgencyLevel(String urgencyLevel) {
    this.urgencyLevel = urgencyLevel;
  }

  public String getHandler() {
    return handler;
  }

  public void setHandler(String handler) {
    this.handler = handler;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }
}
