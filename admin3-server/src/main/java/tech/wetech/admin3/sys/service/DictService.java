package tech.wetech.admin3.sys.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.CommonResultStatus;
import tech.wetech.admin3.sys.model.SysDict;
import tech.wetech.admin3.sys.model.SysDictValue;
import tech.wetech.admin3.sys.repository.SysDictRepository;
import tech.wetech.admin3.sys.repository.SysDictValueRepository;

import java.util.List;

import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;

@Service
public class DictService {

  private final SysDictRepository sysDictRepository;
  private final SysDictValueRepository sysDictValueRepository;

  public DictService(SysDictRepository sysDictRepository, SysDictValueRepository sysDictValueRepository) {
    this.sysDictRepository = sysDictRepository;
    this.sysDictValueRepository = sysDictValueRepository;
  }

  public List<SysDict> findAllDicts() {
    return sysDictRepository.findAll();
  }

  public SysDict findDictById(Long dictId) {
    return sysDictRepository.findById(dictId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
  }

  public SysDict findDictByCode(String dictCode) {
    return sysDictRepository.findByDictCode(dictCode)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
  }

  @Transactional
  public SysDict createDict(String dictCode, String dictName, String description) {
    if (sysDictRepository.existsByDictCode(dictCode)) {
      throw new BusinessException(CommonResultStatus.DICT_CODE_EXISTS);
    }
    SysDict dict = new SysDict();
    dict.setDictCode(dictCode);
    dict.setDictName(dictName);
    dict.setDescription(description);
    return sysDictRepository.save(dict);
  }

  @Transactional
  public SysDict updateDict(Long dictId, String dictCode, String dictName, String description) {
    SysDict dict = sysDictRepository.findById(dictId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!dict.getDictCode().equals(dictCode) && sysDictRepository.existsByDictCode(dictCode)) {
      throw new BusinessException(CommonResultStatus.DICT_CODE_EXISTS);
    }
    dict.setDictCode(dictCode);
    dict.setDictName(dictName);
    dict.setDescription(description);
    return sysDictRepository.save(dict);
  }

  @Transactional
  public void deleteDict(Long dictId) {
    sysDictValueRepository.deleteByDictId(dictId);
    sysDictRepository.deleteById(dictId);
  }

  public List<SysDictValue> findDictValuesByDictId(Long dictId) {
    return sysDictValueRepository.findByDictIdOrderBySortOrderAsc(dictId);
  }

  public List<SysDictValue> findDictValuesByDictCode(String dictCode) {
    SysDict dict = sysDictRepository.findByDictCode(dictCode)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    return sysDictValueRepository.findByDictIdOrderBySortOrderAsc(dict.getId());
  }

  @Transactional
  public SysDictValue createDictValue(Long dictId, String label, String value, Integer sortOrder, String description) {
    SysDict dict = sysDictRepository.findById(dictId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    SysDictValue dictValue = new SysDictValue();
    dictValue.setDict(dict);
    dictValue.setLabel(label);
    dictValue.setValue(value);
    dictValue.setSortOrder(sortOrder);
    dictValue.setDescription(description);
    return sysDictValueRepository.save(dictValue);
  }

  @Transactional
  public SysDictValue updateDictValue(Long valueId, String label, String value, Integer sortOrder, String description) {
    SysDictValue dictValue = sysDictValueRepository.findById(valueId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    dictValue.setLabel(label);
    dictValue.setValue(value);
    dictValue.setSortOrder(sortOrder);
    dictValue.setDescription(description);
    return sysDictValueRepository.save(dictValue);
  }

  @Transactional
  public void deleteDictValue(Long valueId) {
    sysDictValueRepository.deleteById(valueId);
  }

  public String findValueByDictCodeAndLabel(String dictCode, String label) {
    SysDict dict = sysDictRepository.findByDictCode(dictCode)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST, "字典编码不存在: " + dictCode));
    List<SysDictValue> values = sysDictValueRepository.findByDictIdOrderBySortOrderAsc(dict.getId());
    return values.stream()
      .filter(v -> v.getLabel().equals(label))
      .findFirst()
      .map(SysDictValue::getValue)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST, "字典中不存在该标签: " + label));
  }

  public String findLabelByDictCodeAndValue(String dictCode, String value) {
    SysDict dict = sysDictRepository.findByDictCode(dictCode)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST, "字典编码不存在: " + dictCode));
    List<SysDictValue> values = sysDictValueRepository.findByDictIdOrderBySortOrderAsc(dict.getId());
    return values.stream()
      .filter(v -> v.getValue().equals(value))
      .findFirst()
      .map(SysDictValue::getLabel)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST, "字典中不存在该值: " + value));
  }
}
