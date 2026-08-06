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

  @Column(length = 200)
  private String incomingUnit;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(length = 100)
  private String docNo;

  @Column(length = 100)
  private String receivedNo;

  @Column(length = 50)
  private String receivedDate;

  @Column(length = 20)
  private String secrecyLevel;

  @Column(length = 20)
  private String urgencyLevel;

  @Column(length = 100)
  private String handler;

  @Column(columnDefinition = "TEXT")
  private String officeOpinion;

  @Column(columnDefinition = "TEXT")
  private String leaderInstruction;

  @Column(length = 100)
  private String readerSigner;

  @Column(columnDefinition = "TEXT")
  private String handlingStatus;

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

  @Column(nullable = false)
  private Boolean deleted = false;

  @Column
  private LocalDateTime deletedAt;

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

  public String getIncomingUnit() {
    return incomingUnit;
  }

  public void setIncomingUnit(String incomingUnit) {
    this.incomingUnit = incomingUnit;
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

  public String getReceivedNo() {
    return receivedNo;
  }

  public void setReceivedNo(String receivedNo) {
    this.receivedNo = receivedNo;
  }

  public String getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(String receivedDate) {
    this.receivedDate = receivedDate;
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

  public String getOfficeOpinion() {
    return officeOpinion;
  }

  public void setOfficeOpinion(String officeOpinion) {
    this.officeOpinion = officeOpinion;
  }

  public String getLeaderInstruction() {
    return leaderInstruction;
  }

  public void setLeaderInstruction(String leaderInstruction) {
    this.leaderInstruction = leaderInstruction;
  }

  public String getReaderSigner() {
    return readerSigner;
  }

  public void setReaderSigner(String readerSigner) {
    this.readerSigner = readerSigner;
  }

  public String getHandlingStatus() {
    return handlingStatus;
  }

  public void setHandlingStatus(String handlingStatus) {
    this.handlingStatus = handlingStatus;
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
}
