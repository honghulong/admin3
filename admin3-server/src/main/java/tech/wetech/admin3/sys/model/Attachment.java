package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class Attachment extends BaseEntity {

  @Column(nullable = true)
  private Long reimbursementId;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false)
  private Long fileSize;

  @Column(nullable = false, length = 50)
  private String fileType;

  @Column(nullable = false, length = 500)
  private String fileUrl;

  @Column(nullable = false)
  private Long uploadedBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** OCR 状态: pending=待识别, processing=识别中, completed=已完成, failed=识别失败 */
  @Column(length = 20)
  private String ocrStatus;

  /** OCR 识别结果 JSON */
  @Column(columnDefinition = "MEDIUMTEXT")
  private String ocrResult;

  public Long getReimbursementId() {
    return reimbursementId;
  }

  public void setReimbursementId(Long reimbursementId) {
    this.reimbursementId = reimbursementId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public Long getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(Long uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getOcrStatus() {
    return ocrStatus;
  }

  public void setOcrStatus(String ocrStatus) {
    this.ocrStatus = ocrStatus;
  }

  public String getOcrResult() {
    return ocrResult;
  }

  public void setOcrResult(String ocrResult) {
    this.ocrResult = ocrResult;
  }
}
