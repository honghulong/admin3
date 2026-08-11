package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.service.IncomingDocumentService;
import tech.wetech.admin3.sys.service.OcrMappingRuleService;
import tech.wetech.admin3.sys.service.dto.IncomingDocumentDTO;
import tech.wetech.admin3.sys.service.dto.OcrMappingRuleDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/incoming-documents")
public class IncomingDocumentController {

  private final IncomingDocumentService incomingDocumentService;
  private final OcrMappingRuleService ocrMappingRuleService;

  public IncomingDocumentController(IncomingDocumentService incomingDocumentService, OcrMappingRuleService ocrMappingRuleService) {
    this.incomingDocumentService = incomingDocumentService;
    this.ocrMappingRuleService = ocrMappingRuleService;
  }

  @RequiresPermissions("incoming-document:view")
  @GetMapping
  public ResponseEntity<PageDTO<IncomingDocumentDTO>> findDocuments(@RequestParam(required = false) String keyword, Pageable pageable) {
    return ResponseEntity.ok(incomingDocumentService.findDocuments(keyword, pageable));
  }

  @RequiresPermissions("incoming-document:view")
  @GetMapping("/{id}")
  public ResponseEntity<IncomingDocumentDTO> findDocumentById(@PathVariable Long id) {
    return ResponseEntity.ok(incomingDocumentService.findDocumentById(id));
  }

  @RequiresPermissions("incoming-document:create")
  @PostMapping
  public ResponseEntity<IncomingDocumentDTO> createDocument(@RequestBody @Valid IncomingDocumentRequest request) {
    return new ResponseEntity<>(incomingDocumentService.createDocument(toServiceRequest(request)), HttpStatus.CREATED);
  }

  @RequiresPermissions("incoming-document:update")
  @PutMapping("/{id}")
  public ResponseEntity<IncomingDocumentDTO> updateDocument(@PathVariable Long id, @RequestBody @Valid IncomingDocumentRequest request) {
    return ResponseEntity.ok(incomingDocumentService.updateDocument(id, toServiceRequest(request)));
  }

  @RequiresPermissions("incoming-document:delete")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
    incomingDocumentService.deleteDocument(id);
    return ResponseEntity.noContent().build();
  }

  @RequiresPermissions("incoming-document:update")
  @PostMapping("/{id}/attachments")
  public ResponseEntity<IncomingDocumentDTO> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(incomingDocumentService.uploadAttachment(id, file));
  }

  @RequiresPermissions("incoming-document:update")
  @DeleteMapping("/{id}/attachments/{attachmentId}")
  public ResponseEntity<IncomingDocumentDTO> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
    return ResponseEntity.ok(incomingDocumentService.deleteAttachment(id, attachmentId));
  }

  @RequiresPermissions("incoming-document:update")
  @PostMapping("/{id}/scans")
  public ResponseEntity<IncomingDocumentDTO> uploadScan(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(incomingDocumentService.uploadScan(id, file));
  }

  @RequiresPermissions("incoming-document:update")
  @PostMapping("/{id}/scans/{scanId}:recognize")
  public ResponseEntity<IncomingDocumentDTO> recognizeScan(@PathVariable Long id, @PathVariable Long scanId) {
    return ResponseEntity.ok(incomingDocumentService.recognizeScan(id, scanId));
  }

  @RequiresPermissions("incoming-document:view")
  @PostMapping("/{id}/scans/{scanId}:restore")
  public ResponseEntity<IncomingDocumentService.RestoreResponse> restoreScan(
    @PathVariable Long id,
    @PathVariable Long scanId,
    @RequestParam(defaultValue = "false") boolean force
  ) {
    return ResponseEntity.ok(incomingDocumentService.restoreScan(id, scanId, force));
  }

  @RequiresPermissions("incoming-document:update")
  @DeleteMapping("/{id}/scans/{scanId}")
  public ResponseEntity<IncomingDocumentDTO> deleteScan(@PathVariable Long id, @PathVariable Long scanId) {
    return ResponseEntity.ok(incomingDocumentService.deleteScan(id, scanId));
  }

  @RequiresPermissions("incoming-document:view")
  @GetMapping("/ocr-mapping")
  public ResponseEntity<OcrMappingRuleDTO> findMapping() {
    OcrMappingRuleDTO rule = ocrMappingRuleService.findLatestIncomingRule();
    return rule == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(rule);
  }

  @RequiresPermissions("incoming-document:update")
  @PutMapping("/ocr-mapping")
  public ResponseEntity<OcrMappingRuleDTO> saveMapping(@RequestBody @Valid MappingRequest request) {
    if (request.id() == null) {
      return ResponseEntity.ok(ocrMappingRuleService.createRule(
        request.name(),
        OcrMappingRuleService.INCOMING_DOCUMENT_MODULE,
        OcrMappingRuleService.INCOMING_DOCUMENT_FORM,
        request.mappingJson()
      ));
    }
    return ResponseEntity.ok(ocrMappingRuleService.updateRule(
      request.id(),
      request.name(),
      OcrMappingRuleService.INCOMING_DOCUMENT_MODULE,
      OcrMappingRuleService.INCOMING_DOCUMENT_FORM,
      request.mappingJson()
    ));
  }

  private IncomingDocumentService.DocumentRequest toServiceRequest(IncomingDocumentRequest request) {
    return new IncomingDocumentService.DocumentRequest(
      request.code(),
      request.carrierNo(),
      request.incomingUnit(),
      request.title(),
      request.docNo(),
      request.receivedNo(),
      request.receivedDate(),
      request.secrecyLevel(),
      request.urgencyLevel(),
      request.handler(),
      request.officeOpinion(),
      request.leaderInstruction(),
      request.readerSigner(),
      request.handlingStatus(),
      request.remark(),
      request.ocrMappingApplyHistory(),
      request.attachmentIds()
    );
  }

  record IncomingDocumentRequest(
    String code,
    String carrierNo,
    String incomingUnit,
    @NotBlank String title,
    String docNo,
    String receivedNo,
    String receivedDate,
    String secrecyLevel,
    String urgencyLevel,
    String handler,
    String officeOpinion,
    String leaderInstruction,
    String readerSigner,
    String handlingStatus,
    String remark,
    String ocrMappingApplyHistory,
    List<Long> attachmentIds
  ) {}

  record MappingRequest(Long id, @NotBlank String name, String mappingJson) {}
}
