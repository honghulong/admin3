package tech.wetech.admin3.sys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class SysDictValue extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private SysDict dict;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private String value;

  @Column(nullable = false)
  private Integer sortOrder;

  private String description;

  public SysDict getDict() {
    return dict;
  }

  public void setDict(SysDict dict) {
    this.dict = dict;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
