package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.service.ReimbursementService;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/reimbursements")
public class ReimbursementController {

  private static final Logger log = LoggerFactory.getLogger(ReimbursementController.class);

  private final ReimbursementService reimbursementService;

  public ReimbursementController(ReimbursementService reimbursementService) {
    this.reimbursementService = reimbursementService;
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping
  public ResponseEntity<PageDTO<ReimbursementDTO>> findReimbursements(
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Long applicantId,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
    @RequestParam(required = false) String keyword,
    Pageable pageable) {
    log.debug("findReimbursements called: status={}, applicantId={}, category={}, keyword={}, page={}, size={}",
      status, applicantId, category, keyword, pageable.getPageNumber(), pageable.getPageSize());
    return ResponseEntity.ok(reimbursementService.findReimbursements(status, applicantId, category, dateFrom, dateTo, keyword, pageable));
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping("/my")
  public ResponseEntity<PageDTO<ReimbursementDTO>> findMyReimbursements(Pageable pageable) {
    return ResponseEntity.ok(reimbursementService.findMyReimbursements(pageable));
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping("/pending-approvals")
  public ResponseEntity<PageDTO<ReimbursementDTO>> findPendingApprovals(Pageable pageable) {
    return ResponseEntity.ok(reimbursementService.findPendingApprovals(pageable));
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping("/{id}")
  public ResponseEntity<ReimbursementDTO> findReimbursementById(@PathVariable Long id) {
    return ResponseEntity.ok(reimbursementService.findReimbursementById(id));
  }

  @RequiresPermissions("reimbursement:create")
  @PostMapping
  public ResponseEntity<ReimbursementDTO> createReimbursement(@RequestBody @Valid CreateReimbursementRequest request) {
    return new ResponseEntity<>(
      reimbursementService.createReimbursement(request.title(), request.category(), request.amount(), request.description(),
        request.matterDescription(), request.invoiceNo(), request.invoiceCode(), request.invoiceDate(), request.buyerName(), request.sellerName(),
        request.buyerTaxId(), request.sellerTaxId(), request.invoiceType(), request.invoiceStatus(), request.attachmentIds()),
      HttpStatus.CREATED);
  }

  @RequiresPermissions("reimbursement:update")
  @PutMapping("/{id}")
  public ResponseEntity<ReimbursementDTO> updateReimbursement(@PathVariable Long id, @RequestBody @Valid CreateReimbursementRequest request) {
    return ResponseEntity.ok(
      reimbursementService.updateReimbursement(id, request.title(), request.category(), request.amount(), request.description(),
        request.matterDescription(), request.invoiceNo(), request.invoiceCode(), request.invoiceDate(), request.buyerName(), request.sellerName(),
        request.buyerTaxId(), request.sellerTaxId(), request.invoiceType(), request.invoiceStatus(), request.attachmentIds()));
  }

  @RequiresPermissions("reimbursement:update")
  @PutMapping("/{id}/expense-items")
  public ResponseEntity<ReimbursementDTO> saveExpenseItems(@PathVariable Long id, @RequestBody ExpenseItemsRequest request) {
    return ResponseEntity.ok(reimbursementService.saveExpenseItems(id, request.items()));
  }

  @RequiresPermissions("reimbursement:update")
  @PostMapping("/{id}:regroup-expense-items")
  public ResponseEntity<ReimbursementDTO> regroupExpenseItems(@PathVariable Long id) {
    return ResponseEntity.ok(reimbursementService.regroupOcrExpenseItems(id));
  }

  @RequiresPermissions("reimbursement:update")
  @DeleteMapping("/{id}/invoices/{invoiceId}")
  public ResponseEntity<ReimbursementDTO> deleteInvoice(@PathVariable Long id, @PathVariable Long invoiceId) {
    return ResponseEntity.ok(reimbursementService.deleteInvoice(id, invoiceId));
  }

  @RequiresPermissions("reimbursement:update")
  @PostMapping("/{id}/invoices/{invoiceId}/support-attachment")
  public ResponseEntity<ReimbursementDTO> uploadInvoiceSupportAttachment(
    @PathVariable Long id,
    @PathVariable Long invoiceId,
    @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(reimbursementService.uploadInvoiceSupportAttachment(id, invoiceId, file));
  }

  @RequiresPermissions("reimbursement:update")
  @PostMapping("/{id}/support-attachments")
  public ResponseEntity<ReimbursementDTO> uploadSupportAttachment(
    @PathVariable Long id,
    @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(reimbursementService.uploadSupportAttachment(id, file));
  }

  @RequiresPermissions("reimbursement:update")
  @PutMapping("/{id}/invoices/{invoiceId}/support-attachment/{attachmentId}")
  public ResponseEntity<ReimbursementDTO> linkInvoiceSupportAttachment(
    @PathVariable Long id,
    @PathVariable Long invoiceId,
    @PathVariable Long attachmentId) {
    return ResponseEntity.ok(reimbursementService.linkInvoiceSupportAttachment(id, invoiceId, attachmentId));
  }

  @RequiresPermissions("reimbursement:update")
  @DeleteMapping("/{id}/invoices/{invoiceId}/support-attachment")
  public ResponseEntity<ReimbursementDTO> deleteInvoiceSupportAttachment(@PathVariable Long id, @PathVariable Long invoiceId) {
    return ResponseEntity.ok(reimbursementService.deleteInvoiceSupportAttachment(id, invoiceId));
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping("/{id}/exports/electronic-invoice-register")
  public ResponseEntity<byte[]> exportElectronicInvoiceRegister(@PathVariable Long id) {
    return excelResponse("电子发票登记表.xlsx", reimbursementService.exportElectronicInvoiceRegister(id));
  }

  @RequiresPermissions("reimbursement:view")
  @GetMapping("/{id}/exports/expense-details")
  public ResponseEntity<byte[]> exportExpenseDetails(@PathVariable Long id) {
    return excelResponse("报销费用明细表.xlsx", reimbursementService.exportExpenseDetails(id));
  }

  @RequiresPermissions("reimbursement:view")
  @PostMapping("/exports/oa-package")
  public ResponseEntity<byte[]> exportOaPackage(@RequestBody ExportPackageRequest request) {
    String filename = "OA报销表导出包.zip";
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
      .contentType(MediaType.parseMediaType("application/zip"))
      .body(reimbursementService.exportBatchOaExcels(request.ids()));
  }

  @RequiresPermissions("reimbursement:delete")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteReimbursement(@PathVariable Long id) {
    reimbursementService.deleteReimbursement(id);
    return ResponseEntity.noContent().build();
  }

  @RequiresPermissions("reimbursement:submit")
  @PostMapping("/{id}:submit")
  public ResponseEntity<ReimbursementDTO> submitReimbursement(@PathVariable Long id) {
    return ResponseEntity.ok(reimbursementService.submitReimbursement(id));
  }

  @RequiresPermissions("reimbursement:approve")
  @PostMapping("/{id}:approve")
  public ResponseEntity<ReimbursementDTO> approveReimbursement(@PathVariable Long id, @RequestBody(required = false) ApproveRequest request) {
    return ResponseEntity.ok(reimbursementService.approveReimbursement(id, request != null ? request.comment() : null));
  }

  @RequiresPermissions("reimbursement:approve")
  @PostMapping("/{id}:reject")
  public ResponseEntity<ReimbursementDTO> rejectReimbursement(@PathVariable Long id, @RequestBody @Valid ApproveRequest request) {
    return ResponseEntity.ok(reimbursementService.rejectReimbursement(id, request.comment()));
  }

  @RequiresPermissions("reimbursement:recall")
  @PostMapping("/{id}:recall")
  public ResponseEntity<ReimbursementDTO> recallReimbursement(@PathVariable Long id) {
    return ResponseEntity.ok(reimbursementService.recallReimbursement(id));
  }

  record CreateReimbursementRequest(
    @NotBlank String title,
    @NotBlank String category,
    @NotNull BigDecimal amount,
    String description,
    String matterDescription,
    String invoiceNo,
    String invoiceCode,
    String invoiceDate,
    String buyerName,
    String sellerName,
    String buyerTaxId,
    String sellerTaxId,
    String invoiceType,
    String invoiceStatus,
    List<Long> attachmentIds
  ) {}

  record ExpenseItemsRequest(List<ReimbursementService.ExpenseItemRequest> items) {}

  record ExportPackageRequest(List<Long> ids) {}

  record ApproveRequest(String comment) {}

  private ResponseEntity<byte[]> excelResponse(String filename, byte[] content) {
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(content);
  }

}
