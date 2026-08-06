package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.OcrMappingRule;

import java.time.LocalDateTime;

public record OcrMappingRuleDTO(
  Long id,
  String name,
  String module,
  String formType,
  String mappingJson,
  LocalDateTime createdAt,
  Long createdBy,
  LocalDateTime updatedAt,
  Long updatedBy,
  Boolean deleted
) {

  public static OcrMappingRuleDTO of(OcrMappingRule rule) {
    return new OcrMappingRuleDTO(
      rule.getId(),
      rule.getName(),
      rule.getModule(),
      rule.getFormType(),
      rule.getMappingJson(),
      rule.getCreatedAt(),
      rule.getCreatedBy(),
      rule.getUpdatedAt(),
      rule.getUpdatedBy(),
      rule.getDeleted()
    );
  }
}
