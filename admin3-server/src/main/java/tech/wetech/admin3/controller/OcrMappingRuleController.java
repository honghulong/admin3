package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.service.OcrMappingRuleService;
import tech.wetech.admin3.sys.service.dto.OcrMappingRuleDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/ocr-mapping-rules")
public class OcrMappingRuleController {

  private final OcrMappingRuleService ruleService;

  public OcrMappingRuleController(OcrMappingRuleService ruleService) {
    this.ruleService = ruleService;
  }

  @RequiresPermissions("ocr-mapping-rule:view")
  @GetMapping
  public ResponseEntity<PageDTO<OcrMappingRuleDTO>> findRules(@RequestParam(required = false) String keyword, Pageable pageable) {
    return ResponseEntity.ok(ruleService.findRules(keyword, pageable));
  }

  @RequiresPermissions("ocr-mapping-rule:view")
  @GetMapping("/{id}")
  public ResponseEntity<OcrMappingRuleDTO> findRuleById(@PathVariable Long id) {
    return ResponseEntity.ok(ruleService.findRuleById(id));
  }

  @RequiresPermissions("ocr-mapping-rule:view")
  @GetMapping("/incoming-document/latest")
  public ResponseEntity<OcrMappingRuleDTO> findLatestIncomingRule() {
    OcrMappingRuleDTO rule = ruleService.findLatestIncomingRule();
    return rule == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(rule);
  }

  @RequiresPermissions("ocr-mapping-rule:update")
  @PostMapping
  public ResponseEntity<OcrMappingRuleDTO> createRule(@RequestBody @Valid OcrMappingRuleRequest request) {
    return new ResponseEntity<>(ruleService.createRule(request.name(), request.module(), request.formType(), request.mappingJson()), HttpStatus.CREATED);
  }

  @RequiresPermissions("ocr-mapping-rule:update")
  @PutMapping("/{id}")
  public ResponseEntity<OcrMappingRuleDTO> updateRule(@PathVariable Long id, @RequestBody @Valid OcrMappingRuleRequest request) {
    return ResponseEntity.ok(ruleService.updateRule(id, request.name(), request.module(), request.formType(), request.mappingJson()));
  }

  @RequiresPermissions("ocr-mapping-rule:delete")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
    ruleService.deleteRule(id);
    return ResponseEntity.noContent().build();
  }

  record OcrMappingRuleRequest(
    @NotBlank String name,
    String module,
    String formType,
    String mappingJson
  ) {}
}
