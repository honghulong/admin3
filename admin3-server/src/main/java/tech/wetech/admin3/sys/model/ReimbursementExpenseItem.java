package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class ReimbursementExpenseItem extends BaseEntity {

  @Column(nullable = false)
  private Long reimbursementId;

  private Long invoiceId;

  private LocalDateTime expenseDate;

  @Column(length = 100)
  private String startPlace;

  @Column(length = 100)
  private String endPlace;

  @Column(precision = 10, scale = 2)
  private BigDecimal kilometers;

  @Column(nullable = false, length = 50)
  private String expenseType;

  @Column(length = 100)
  private String oaExpenseName;

  @Column(precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(length = 200)
  private String projectCostCategory;

  @Column(length = 200)
  private String customerName;

  @Column(columnDefinition = "TEXT")
  private String remark;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 20)
  private String source;

  private Integer sortOrder;

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

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public LocalDateTime getExpenseDate() {
    return expenseDate;
  }

  public void setExpenseDate(LocalDateTime expenseDate) {
    this.expenseDate = expenseDate;
  }

  public String getStartPlace() {
    return startPlace;
  }

  public void setStartPlace(String startPlace) {
    this.startPlace = startPlace;
  }

  public String getEndPlace() {
    return endPlace;
  }

  public void setEndPlace(String endPlace) {
    this.endPlace = endPlace;
  }

  public BigDecimal getKilometers() {
    return kilometers;
  }

  public void setKilometers(BigDecimal kilometers) {
    this.kilometers = kilometers;
  }

  public String getExpenseType() {
    return expenseType;
  }

  public void setExpenseType(String expenseType) {
    this.expenseType = expenseType;
  }

  public String getOaExpenseName() {
    return oaExpenseName;
  }

  public void setOaExpenseName(String oaExpenseName) {
    this.oaExpenseName = oaExpenseName;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getProjectCostCategory() {
    return projectCostCategory;
  }

  public void setProjectCostCategory(String projectCostCategory) {
    this.projectCostCategory = projectCostCategory;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
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
