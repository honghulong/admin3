package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.model.OcrEngineConfig;
import tech.wetech.admin3.sys.repository.OcrEngineConfigRepository;
import tech.wetech.admin3.sys.service.ocr.OcrSecretCipher;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/ocr-engine-configs")
public class OcrEngineConfigController {

  private final OcrEngineConfigRepository repository;
  private final OcrSecretCipher secretCipher;

  public OcrEngineConfigController(OcrEngineConfigRepository repository, OcrSecretCipher secretCipher) {
    this.repository = repository;
    this.secretCipher = secretCipher;
  }

  @RequiresPermissions("ocr-engine:view")
  @GetMapping
  public ResponseEntity<List<OcrEngineConfigResponse>> list() {
    return ResponseEntity.ok(repository.findAll().stream().map(OcrEngineConfigResponse::from).toList());
  }

  @RequiresPermissions("ocr-engine:create")
  @PostMapping
  public ResponseEntity<OcrEngineConfigResponse> create(@RequestBody @Valid OcrEngineConfigRequest request) {
    OcrEngineConfig config = new OcrEngineConfig();
    apply(config, request, true);
    return new ResponseEntity<>(OcrEngineConfigResponse.from(repository.save(config)), HttpStatus.CREATED);
  }

  @RequiresPermissions("ocr-engine:update")
  @PutMapping("/{id}")
  public ResponseEntity<OcrEngineConfigResponse> update(@PathVariable Long id, @RequestBody @Valid OcrEngineConfigRequest request) {
    OcrEngineConfig config = repository.findById(id).orElseThrow();
    apply(config, request, false);
    return ResponseEntity.ok(OcrEngineConfigResponse.from(repository.save(config)));
  }

  @RequiresPermissions("ocr-engine:delete")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    repository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @RequiresPermissions("ocr-engine:markAsDefault")
  @PostMapping("/{id}:markAsDefault")
  public ResponseEntity<OcrEngineConfigResponse> markAsDefault(@PathVariable Long id) {
    OcrEngineConfig selected = repository.findById(id).orElseThrow();
    LocalDateTime now = LocalDateTime.now();
    repository.findAll().forEach(config -> {
      config.setIsDefault(config.getId().equals(id));
      config.setEnabled(config.getId().equals(id));
      config.setUpdatedAt(now);
      repository.save(config);
    });
    selected.setIsDefault(true);
    selected.setEnabled(true);
    return ResponseEntity.ok(OcrEngineConfigResponse.from(selected));
  }

  private void apply(OcrEngineConfig config, OcrEngineConfigRequest request, boolean create) {
    LocalDateTime now = LocalDateTime.now();
    boolean enabled = request.enabled() == null || request.enabled();
    validateBaseUrl(request.providerType(), request.baseUrl());
    if (enabled) {
      repository.findAll().forEach(existing -> {
        if (config.getId() == null || !existing.getId().equals(config.getId())) {
          existing.setIsDefault(false);
          existing.setEnabled(false);
          existing.setUpdatedAt(now);
          repository.save(existing);
        }
      });
    }
    config.setName(request.name());
    config.setProviderType(request.providerType());
    config.setBaseUrl(request.baseUrl());
    config.setModelName(request.modelName());
    config.setProtocol(request.protocol());
    config.setTenantId(request.tenantId());
    config.setEnvironment(request.environment());
    config.setPrompt(request.prompt());
    config.setEnabled(enabled);
    config.setIsDefault(enabled);
    config.setFallbackEnabled(request.fallbackEnabled() == null || request.fallbackEnabled());
    if (request.apiKey() != null && !request.apiKey().isBlank()) {
      config.setApiKeyCipherText(secretCipher.encrypt(request.apiKey()));
    }
    if (create) config.setCreatedAt(now);
    config.setUpdatedAt(now);
  }

  private void validateBaseUrl(String providerType, String baseUrl) {
    if ("INTERNAL".equalsIgnoreCase(providerType)) return;
    if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("OCR engine baseUrl is required");
    if (baseUrl.contains("{") || baseUrl.contains("}")) {
      throw new IllegalArgumentException("OCR engine baseUrl cannot contain placeholders");
    }
    URI uri = URI.create(baseUrl);
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException("OCR engine baseUrl must be an absolute URL");
    }
  }

  public record OcrEngineConfigRequest(
    @NotBlank String name,
    @NotBlank String providerType,
    String baseUrl,
    String modelName,
    String apiKey,
    String protocol,
    String tenantId,
    String environment,
    String prompt,
    Boolean enabled,
    Boolean isDefault,
    Boolean fallbackEnabled
  ) {}

  public record OcrEngineConfigResponse(
    Long id,
    String name,
    String providerType,
    String baseUrl,
    String modelName,
    String protocol,
    String tenantId,
    String environment,
    String prompt,
    Boolean enabled,
    Boolean isDefault,
    Boolean fallbackEnabled,
    Boolean apiKeyConfigured,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
  ) {
    static OcrEngineConfigResponse from(OcrEngineConfig config) {
      return new OcrEngineConfigResponse(
        config.getId(), config.getName(), config.getProviderType(), config.getBaseUrl(),
        config.getModelName(), config.getProtocol(), config.getTenantId(), config.getEnvironment(),
        config.getPrompt(), config.getEnabled(), config.getIsDefault(), config.getFallbackEnabled(),
        config.getApiKeyCipherText() != null && !config.getApiKeyCipherText().isBlank(),
        config.getCreatedAt(), config.getUpdatedAt()
      );
    }
  }
}
