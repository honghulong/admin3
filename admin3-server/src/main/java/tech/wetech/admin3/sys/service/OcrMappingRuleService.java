package tech.wetech.admin3.sys.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.sys.model.OcrMappingRule;
import tech.wetech.admin3.sys.repository.OcrMappingRuleRepository;
import tech.wetech.admin3.sys.service.dto.OcrMappingRuleDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.time.LocalDateTime;

import static tech.wetech.admin3.common.CommonResultStatus.PARAM_ERROR;
import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@Service
@Transactional
public class OcrMappingRuleService {

  public static final String INCOMING_DOCUMENT_MODULE = "incoming-document";
  public static final String INCOMING_DOCUMENT_FORM = "incoming-document-form";

  private final OcrMappingRuleRepository ruleRepository;

  public OcrMappingRuleService(OcrMappingRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  public PageDTO<OcrMappingRuleDTO> findRules(String keyword, Pageable pageable) {
    Page<OcrMappingRule> page = ruleRepository.search(keyword, pageable);
    return new PageDTO<>(page.map(OcrMappingRuleDTO::of).getContent(), page.getTotalElements());
  }

  public OcrMappingRuleDTO findRuleById(Long id) {
    return OcrMappingRuleDTO.of(ruleRepository.findById(id)
      .filter(rule -> !Boolean.TRUE.equals(rule.getDeleted()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST)));
  }

  public OcrMappingRuleDTO findLatestIncomingRule() {
    return ruleRepository.findFirstByModuleAndDeletedFalseOrderByUpdatedAtDesc(INCOMING_DOCUMENT_MODULE)
      .map(OcrMappingRuleDTO::of)
      .orElse(null);
  }

  public OcrMappingRuleDTO createRule(String name, String module, String formType, String mappingJson) {
    validateName(name, null);
    UserinfoDTO user = currentUser();
    LocalDateTime now = LocalDateTime.now();
    OcrMappingRule rule = new OcrMappingRule();
    rule.setName(name.trim());
    rule.setModule(firstNonBlank(module, INCOMING_DOCUMENT_MODULE));
    rule.setFormType(firstNonBlank(formType, INCOMING_DOCUMENT_FORM));
    rule.setMappingJson(normalizeMappingJson(mappingJson));
    rule.setCreatedAt(now);
    rule.setCreatedBy(user.userId());
    rule.setUpdatedAt(now);
    rule.setUpdatedBy(user.userId());
    rule.setDeleted(false);
    return OcrMappingRuleDTO.of(ruleRepository.save(rule));
  }

  public OcrMappingRuleDTO updateRule(Long id, String name, String module, String formType, String mappingJson) {
    OcrMappingRule rule = ruleRepository.findById(id)
      .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    validateName(name, id);
    rule.setName(name.trim());
    rule.setModule(firstNonBlank(module, INCOMING_DOCUMENT_MODULE));
    rule.setFormType(firstNonBlank(formType, INCOMING_DOCUMENT_FORM));
    rule.setMappingJson(normalizeMappingJson(mappingJson));
    rule.setUpdatedAt(LocalDateTime.now());
    rule.setUpdatedBy(currentUser().userId());
    return OcrMappingRuleDTO.of(ruleRepository.save(rule));
  }

  public void deleteRule(Long id) {
    OcrMappingRule rule = ruleRepository.findById(id).orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    rule.setDeleted(true);
    rule.setDeletedAt(LocalDateTime.now());
    rule.setUpdatedAt(LocalDateTime.now());
    rule.setUpdatedBy(currentUser().userId());
    ruleRepository.save(rule);
  }

  private void validateName(String name, Long id) {
    if (name == null || name.isBlank()) throw new BusinessException(PARAM_ERROR, "映射规则名称不能为空");
    boolean exists = id == null
      ? ruleRepository.existsByNameAndDeletedFalse(name.trim())
      : ruleRepository.existsByNameAndIdNotAndDeletedFalse(name.trim(), id);
    if (exists) throw new BusinessException(PARAM_ERROR, "映射规则名称已存在");
  }

  private String normalizeMappingJson(String mappingJson) {
    return mappingJson == null || mappingJson.isBlank() ? "[]" : mappingJson;
  }

  private UserinfoDTO currentUser() {
    return (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }
}
