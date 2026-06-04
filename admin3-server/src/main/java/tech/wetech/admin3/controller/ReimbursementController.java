package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.service.ReimbursementService;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;

import java.math.BigDecimal;
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
        request.invoiceNo(), request.invoiceCode(), request.invoiceDate(), request.buyerName(), request.sellerName(),
        request.buyerTaxId(), request.sellerTaxId(), request.invoiceType(), request.invoiceStatus(), request.attachmentIds()),
      HttpStatus.CREATED);
  }

  @RequiresPermissions("reimbursement:update")
  @PutMapping("/{id}")
  public ResponseEntity<ReimbursementDTO> updateReimbursement(@PathVariable Long id, @RequestBody @Valid CreateReimbursementRequest request) {
    return ResponseEntity.ok(
      reimbursementService.updateReimbursement(id, request.title(), request.category(), request.amount(), request.description(),
        request.invoiceNo(), request.invoiceCode(), request.invoiceDate(), request.buyerName(), request.sellerName(),
        request.buyerTaxId(), request.sellerTaxId(), request.invoiceType(), request.invoiceStatus(), request.attachmentIds()));
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

  record ApproveRequest(String comment) {}

}
