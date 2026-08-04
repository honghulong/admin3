package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class OcrMappingConfig extends BaseEntity {

  @Column(nullable = false, length = 80)
  private String module;

  @Column
  private Long userId;

  @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
  private String mappingJson;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getMappingJson() {
    return mappingJson;
  }

  public void setMappingJson(String mappingJson) {
    this.mappingJson = mappingJson;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
