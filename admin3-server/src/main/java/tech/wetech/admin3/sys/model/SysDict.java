package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class SysDict extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String dictCode;

  @Column(nullable = false)
  private String dictName;

  private String description;

  @OneToMany(mappedBy = "dict")
  @OrderBy("sortOrder asc")
  private Set<SysDictValue> dictValues = new LinkedHashSet<>();

  public String getDictCode() {
    return dictCode;
  }

  public void setDictCode(String dictCode) {
    this.dictCode = dictCode;
  }

  public String getDictName() {
    return dictName;
  }

  public void setDictName(String dictName) {
    this.dictName = dictName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<SysDictValue> getDictValues() {
    return dictValues;
  }

  public void setDictValues(Set<SysDictValue> dictValues) {
    this.dictValues = dictValues;
  }
}
