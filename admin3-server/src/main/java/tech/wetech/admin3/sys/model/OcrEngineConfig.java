package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class OcrEngineConfig extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 30)
  private String providerType;

  @Column(length = 500)
  private String baseUrl;

  @Column(length = 100)
  private String modelName;

  @Column(columnDefinition = "TEXT")
  private String apiKeyCipherText;

  @Column(length = 30)
  private String protocol;

  @Column(length = 100)
  private String tenantId;

  @Column(length = 50)
  private String environment;

  @Column(columnDefinition = "TEXT")
  private String prompt;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @Column(nullable = false)
  private Boolean fallbackEnabled = true;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getApiKeyCipherText() {
    return apiKeyCipherText;
  }

  public void setApiKeyCipherText(String apiKeyCipherText) {
    this.apiKeyCipherText = apiKeyCipherText;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean aDefault) {
    isDefault = aDefault;
  }

  public Boolean getFallbackEnabled() {
    return fallbackEnabled;
  }

  public void setFallbackEnabled(Boolean fallbackEnabled) {
    this.fallbackEnabled = fallbackEnabled;
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
