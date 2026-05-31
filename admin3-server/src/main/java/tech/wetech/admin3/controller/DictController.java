package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import org.springframework.web.bind.annotation.*;
import tech.wetech.admin3.sys.model.SysDict;
import tech.wetech.admin3.sys.model.SysDictValue;
import tech.wetech.admin3.sys.service.DictService;

import java.util.List;
import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/dict")
public class DictController {

  private final DictService dictService;

  public DictController(DictService dictService) {
    this.dictService = dictService;
  }

  @GetMapping
  @RequiresPermissions("dict:view")
  public ResponseEntity<List<SysDict>> findAllDicts() {
    return ResponseEntity.ok(dictService.findAllDicts());
  }

  @GetMapping("/{dictId}")
  @RequiresPermissions("dict:view")
  public ResponseEntity<SysDict> findDictById(@PathVariable Long dictId) {
    return ResponseEntity.ok(dictService.findDictById(dictId));
  }

  @GetMapping("/code/{dictCode}")
  public ResponseEntity<SysDict> findDictByCode(@PathVariable String dictCode) {
    return ResponseEntity.ok(dictService.findDictByCode(dictCode));
  }

  @PostMapping
  @RequiresPermissions("dict:create")
  public ResponseEntity<SysDict> createDict(@RequestBody Map<String, String> body) {
    return ResponseEntity.ok(dictService.createDict(body.get("dictCode"), body.get("dictName"), body.get("description")));
  }

  @PutMapping("/{dictId}")
  @RequiresPermissions("dict:update")
  public ResponseEntity<SysDict> updateDict(@PathVariable Long dictId, @RequestBody Map<String, String> body) {
    return ResponseEntity.ok(dictService.updateDict(dictId, body.get("dictCode"), body.get("dictName"), body.get("description")));
  }

  @DeleteMapping("/{dictId}")
  @RequiresPermissions("dict:delete")
  public ResponseEntity<Void> deleteDict(@PathVariable Long dictId) {
    dictService.deleteDict(dictId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{dictId}/values")
  @RequiresPermissions("dict:view")
  public ResponseEntity<List<SysDictValue>> findDictValues(@PathVariable Long dictId) {
    return ResponseEntity.ok(dictService.findDictValuesByDictId(dictId));
  }

  @GetMapping("/code/{dictCode}/values")
  public ResponseEntity<List<SysDictValue>> findDictValuesByCode(@PathVariable String dictCode) {
    return ResponseEntity.ok(dictService.findDictValuesByDictCode(dictCode));
  }

  @PostMapping("/{dictId}/values")
  @RequiresPermissions("dict:create")
  public ResponseEntity<SysDictValue> createDictValue(@PathVariable Long dictId, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(dictService.createDictValue(
      dictId,
      (String) body.get("label"),
      (String) body.get("value"),
      (Integer) body.get("sortOrder"),
      (String) body.get("description")
    ));
  }

  @PutMapping("/values/{valueId}")
  @RequiresPermissions("dict:update")
  public ResponseEntity<SysDictValue> updateDictValue(@PathVariable Long valueId, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(dictService.updateDictValue(
      valueId,
      (String) body.get("label"),
      (String) body.get("value"),
      (Integer) body.get("sortOrder"),
      (String) body.get("description")
    ));
  }

  @DeleteMapping("/values/{valueId}")
  @RequiresPermissions("dict:delete")
  public ResponseEntity<Void> deleteDictValue(@PathVariable Long valueId) {
    dictService.deleteDictValue(valueId);
    return ResponseEntity.noContent().build();
  }
}
