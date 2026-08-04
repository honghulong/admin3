package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class IncomingScanAttachment extends BaseEntity {

  @Column(nullable = false)
  private Long incomingDocumentId;

  @Column(nullable = false)
  private Long attachmentId;

  @Column(length = 20)
  private String ocrStatus;

  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrRawJson;

  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrBusinessJson;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private Long createdBy;

  @Column(nullable = false)
  private Boolean deleted = false;

  @Column
  private LocalDateTime deletedAt;

  public Long getIncomingDocumentId() {
    return incomingDocumentId;
  }

  public void setIncomingDocumentId(Long incomingDocumentId) {
    this.incomingDocumentId = incomingDocumentId;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public void setAttachmentId(Long attachmentId) {
    this.attachmentId = attachmentId;
  }

  public String getOcrStatus() {
    return ocrStatus;
  }

  public void setOcrStatus(String ocrStatus) {
    this.ocrStatus = ocrStatus;
  }

  public String getOcrRawJson() {
    return ocrRawJson;
  }

  public void setOcrRawJson(String ocrRawJson) {
    this.ocrRawJson = ocrRawJson;
  }

  public String getOcrBusinessJson() {
    return ocrBusinessJson;
  }

  public void setOcrBusinessJson(String ocrBusinessJson) {
    this.ocrBusinessJson = ocrBusinessJson;
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
