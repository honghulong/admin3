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
import tech.wetech.admin3.sys.service.dto.IncomingDocumentDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/incoming-documents")
public class IncomingDocumentController {

  private final IncomingDocumentService incomingDocumentService;

  public IncomingDocumentController(IncomingDocumentService incomingDocumentService) {
    this.incomingDocumentService = incomingDocumentService;
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

  @RequiresPermissions("incoming-document:update")
  @DeleteMapping("/{id}/scans/{scanId}")
  public ResponseEntity<IncomingDocumentDTO> deleteScan(@PathVariable Long id, @PathVariable Long scanId) {
    return ResponseEntity.ok(incomingDocumentService.deleteScan(id, scanId));
  }

  @RequiresPermissions("incoming-document:view")
  @GetMapping("/ocr-mapping")
  public ResponseEntity<MappingResponse> findMapping() {
    return ResponseEntity.ok(new MappingResponse(incomingDocumentService.findMapping()));
  }

  @RequiresPermissions("incoming-document:update")
  @PutMapping("/ocr-mapping")
  public ResponseEntity<MappingResponse> saveMapping(@RequestBody MappingRequest request) {
    return ResponseEntity.ok(new MappingResponse(incomingDocumentService.saveMapping(request.mappingJson())));
  }

  private IncomingDocumentService.DocumentRequest toServiceRequest(IncomingDocumentRequest request) {
    return new IncomingDocumentService.DocumentRequest(
      request.code(),
      request.carrierNo(),
      request.title(),
      request.docNo(),
      request.secrecyLevel(),
      request.urgencyLevel(),
      request.handler(),
      request.remark(),
      request.attachmentIds()
    );
  }

  record IncomingDocumentRequest(
    String code,
    String carrierNo,
    @NotBlank String title,
    String docNo,
    String secrecyLevel,
    String urgencyLevel,
    String handler,
    String remark,
    List<Long> attachmentIds
  ) {}

  record MappingRequest(String mappingJson) {}

  record MappingResponse(String mappingJson) {}
}
