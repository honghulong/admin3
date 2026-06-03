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

  @Column(columnDefinition = "TEXT")
  private String description;

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

  @Column(nullable = false)
  private LocalDateTime createdAt;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
