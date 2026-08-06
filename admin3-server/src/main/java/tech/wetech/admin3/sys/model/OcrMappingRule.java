package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class OcrMappingRule extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(nullable = false, length = 80)
  private String module;

  @Column(length = 80)
  private String formType;

  @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
  private String mappingJson;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public String getFormType() {
    return formType;
  }

  public void setFormType(String formType) {
    this.formType = formType;
  }

  public String getMappingJson() {
    return mappingJson;
  }

  public void setMappingJson(String mappingJson) {
    this.mappingJson = mappingJson;
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
