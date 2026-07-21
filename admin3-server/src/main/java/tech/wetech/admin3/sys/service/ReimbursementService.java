package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.CommonResultStatus;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.InvoiceTemp;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.ApprovalLogRepository;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import tech.wetech.admin3.sys.repository.InvoiceTempRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;
import tech.wetech.admin3.sys.repository.ReimbursementExpenseItemRepository;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static tech.wetech.admin3.common.CommonResultStatus.PARAM_ERROR;
import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@Service
@Transactional
public class ReimbursementService {

  private final ReimbursementRepository reimbursementRepository;
  private final AttachmentRepository attachmentRepository;
  private final ApprovalLogRepository approvalLogRepository;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceTempRepository invoiceTempRepository;
  private final ReimbursementExpenseItemRepository expenseItemRepository;
  private final ObjectMapper objectMapper;
  private final OcrService ocrService;
  private final Path uploadDir = Paths.get("uploads/attachments");

  public ReimbursementService(ReimbursementRepository reimbursementRepository,
                              AttachmentRepository attachmentRepository,
                              ApprovalLogRepository approvalLogRepository,
                              InvoiceRepository invoiceRepository,
                              InvoiceTempRepository invoiceTempRepository,
                              ReimbursementExpenseItemRepository expenseItemRepository,
                              ObjectMapper objectMapper,
                              OcrService ocrService) {
    this.reimbursementRepository = reimbursementRepository;
    this.attachmentRepository = attachmentRepository;
    this.approvalLogRepository = approvalLogRepository;
    this.invoiceRepository = invoiceRepository;
    this.invoiceTempRepository = invoiceTempRepository;
    this.expenseItemRepository = expenseItemRepository;
    this.objectMapper = objectMapper;
    this.ocrService = ocrService;
  }

  public PageDTO<ReimbursementDTO> findReimbursements(String status, Long applicantId, String category,
                                                       LocalDateTime dateFrom, LocalDateTime dateTo,
                                                       String keyword, Pageable pageable) {
    Page<Reimbursement> page = reimbursementRepository.searchReimbursements(
      status, applicantId, category, dateFrom, dateTo, keyword, pageable);
    Page<ReimbursementDTO> dtoPage = page.map(ReimbursementDTO::brief);
    return new PageDTO<>(dtoPage.getContent(), dtoPage.getTotalElements());
  }

  public PageDTO<ReimbursementDTO> findMyReimbursements(Pageable pageable) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    Page<Reimbursement> page = reimbursementRepository.findByApplicantIdOrderByCreatedAtDesc(userInfo.userId(), pageable);
    Page<ReimbursementDTO> dtoPage = page.map(ReimbursementDTO::brief);
    return new PageDTO<>(dtoPage.getContent(), dtoPage.getTotalElements());
  }

  public PageDTO<ReimbursementDTO> findPendingApprovals(Pageable pageable) {
    Page<Reimbursement> page = reimbursementRepository.findByStatusOrderByCreatedAtDesc("pending", pageable);
    Page<ReimbursementDTO> dtoPage = page.map(ReimbursementDTO::brief);
    return new PageDTO<>(dtoPage.getContent(), dtoPage.getTotalElements());
  }

  public PageDTO<ReimbursementDTO> findMyApprovals(Pageable pageable) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    Page<Reimbursement> page = reimbursementRepository.findByApplicantIdOrderByCreatedAtDesc(userInfo.userId(), pageable);
    Page<ReimbursementDTO> dtoPage = page.map(ReimbursementDTO::brief);
    return new PageDTO<>(dtoPage.getContent(), dtoPage.getTotalElements());
  }

  public List<ReimbursementDTO> findReimbursementsByUsername(String username) {
    List<Reimbursement> list = reimbursementRepository.findByApplicantNameOrderByCreatedAtDesc(username);
    return list.stream().map(ReimbursementDTO::brief).toList();
  }

  public ReimbursementDTO findReimbursementById(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    updateExpenseSummary(r);
    reimbursementRepository.save(r);
    List<Invoice> allInvoices = invoiceRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    List<Attachment> attachments = filterActiveAttachments(
      attachmentRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(id),
      allInvoices);
    List<ApprovalLog> logs = approvalLogRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(id);
    List<ReimbursementExpenseItem> expenseItems = expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(id);
    List<InvoiceTemp> invoiceTemps = invoiceTempRepository.findByReimbursementIdOrderByCreatedAtDesc(id);
    return ReimbursementDTO.of(r, attachments, logs, invoices, expenseItems, invoiceTemps);
  }

  private List<Attachment> filterActiveAttachments(List<Attachment> attachments, List<Invoice> allInvoices) {
    Map<Long, List<Invoice>> invoicesByAttachmentId = allInvoices.stream()
      .filter(invoice -> invoice.getAttachmentId() != null)
      .collect(Collectors.groupingBy(Invoice::getAttachmentId));
    return attachments.stream()
      .filter(attachment -> {
        List<Invoice> relatedInvoices = invoicesByAttachmentId.get(attachment.getId());
        return relatedInvoices == null || relatedInvoices.stream().anyMatch(invoice -> !Boolean.TRUE.equals(invoice.getDeleted()));
      })
      .toList();
  }

  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description, List<Long> attachmentIds) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    return createReimbursement(title, category, amount, description, null, attachmentIds,
      userInfo.userId(), userInfo.username(), null, null, null,
      null, null, null, null, null, null, null);
  }

  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description,
                                              String matterDescription,
                                              String invoiceNo, String invoiceCode, String invoiceDate,
                                              String buyerName, String sellerName,
                                              String buyerTaxId, String sellerTaxId,
                                              String invoiceType, String invoiceStatus,
                                              List<Long> attachmentIds) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime date = invoiceDate != null ? LocalDateTime.parse(invoiceDate) : null;
    return createReimbursement(title, category, amount, description, matterDescription, attachmentIds,
      userInfo.userId(), userInfo.username(), invoiceNo, invoiceCode, date,
      buyerName, sellerName, buyerTaxId, sellerTaxId, invoiceType, invoiceStatus,
      null);
  }

  /**
   * 创建报销单（支持 MCP 无 Session 场景，直接传入用户信息）
   */
  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description, String matterDescription, List<Long> attachmentIds,
                                              Long applicantId, String applicantName,
                                              String invoiceNo, String invoiceCode,
                                              LocalDateTime invoiceDate, String buyerName,
                                              String sellerName, String buyerTaxId,
                                              String sellerTaxId, String invoiceType,
                                              String invoiceStatus,
                                              String ocrRawJson) {
    LocalDateTime now = LocalDateTime.now();

    Reimbursement r = new Reimbursement();
    r.setTitle(title);
    r.setCategory(category);
    r.setAmount(amount);
    r.setInvoiceNo(invoiceNo);
    r.setInvoiceCode(invoiceCode);
    r.setInvoiceDate(invoiceDate);
    r.setBuyerName(buyerName);
    r.setSellerName(sellerName);
    r.setBuyerTaxId(buyerTaxId);
    r.setSellerTaxId(sellerTaxId);
    r.setInvoiceType(invoiceType);
    r.setInvoiceStatus(invoiceStatus);
    r.setDescription(description);
    r.setMatterDescription(matterDescription);
    r.setOcrRawJson(ocrRawJson);
    r.setStatus("draft");
    r.setApplicantId(applicantId);
    r.setApplicantName(applicantName);
    r.setCreatedBy(applicantId);
    r.setCreatedAt(now);
    r.setUpdatedBy(applicantId);
    r.setUpdatedAt(now);
    Reimbursement saved = reimbursementRepository.save(r);
    final Long savedId = saved.getId();

    // 关联附件
    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      for (Long attachmentId : attachmentIds) {
        attachmentRepository.findById(attachmentId).ifPresent(a -> {
          a.setReimbursementId(savedId);
          attachmentRepository.save(a);
        });
      }
    }

    return findReimbursementById(savedId);
  }

  public ReimbursementDTO updateReimbursement(Long id, String title, String category, BigDecimal amount,
                                              String description, List<Long> attachmentIds) {
    return updateReimbursement(id, title, category, amount, description, null,
      null, null, null, null, null, null, null, null, null, attachmentIds);
  }

  public ReimbursementDTO updateReimbursement(Long id, String title, String category, BigDecimal amount,
                                              String description,
                                              String matterDescription,
                                              String invoiceNo, String invoiceCode, String invoiceDate,
                                              String buyerName, String sellerName,
                                              String buyerTaxId, String sellerTaxId,
                                              String invoiceType, String invoiceStatus,
                                              List<Long> attachmentIds) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "当前状态不可编辑");
    }
    r.setTitle(title);
    r.setCategory(category);
    r.setAmount(amount);
    r.setDescription(description);
    r.setMatterDescription(matterDescription);
    if (invoiceNo != null) r.setInvoiceNo(invoiceNo);
    if (invoiceCode != null) r.setInvoiceCode(invoiceCode);
    if (invoiceDate != null) r.setInvoiceDate(LocalDateTime.parse(invoiceDate));
    if (buyerName != null) r.setBuyerName(buyerName);
    if (sellerName != null) r.setSellerName(sellerName);
    if (buyerTaxId != null) r.setBuyerTaxId(buyerTaxId);
    if (sellerTaxId != null) r.setSellerTaxId(sellerTaxId);
    if (invoiceType != null) r.setInvoiceType(invoiceType);
    if (invoiceStatus != null) r.setInvoiceStatus(invoiceStatus);
    r.setUpdatedAt(LocalDateTime.now());
    reimbursementRepository.save(r);

    // 更新附件关联
    if (attachmentIds != null) {
      Set<Long> supportAttachmentIds = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(id).stream()
        .map(Invoice::getSupportAttachmentId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
      // 解除旧关联
      List<Attachment> oldAttachments = attachmentRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(id);
      for (Attachment a : oldAttachments) {
        if (supportAttachmentIds.contains(a.getId())) continue;
        a.setReimbursementId(null);
        attachmentRepository.save(a);
      }
      // 建立新关联
      for (Long attachmentId : attachmentIds) {
        attachmentRepository.findById(attachmentId).ifPresent(a -> {
          a.setReimbursementId(id);
          attachmentRepository.save(a);
        });
      }
    }
    deduplicateInvoicesOnSave(id);

    return findReimbursementById(id);
  }

  public void deleteReimbursement(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "当前状态不可删除");
    }
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    invoiceRepository.deleteAll(invoices);
    attachmentRepository.deleteByReimbursementId(id);
    expenseItemRepository.deleteByReimbursementId(id);
    List<ApprovalLog> logs = approvalLogRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    approvalLogRepository.deleteAll(logs);
    reimbursementRepository.delete(r);
  }

  public ReimbursementDTO deleteInvoice(Long reimbursementId, Long invoiceId) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "当前状态不可删除发票");
    }
    Invoice invoice = invoiceRepository.findById(invoiceId)
      .filter(item -> reimbursementId.equals(item.getReimbursementId()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    Long affectedExpenseItemId = invoice.getExpenseItemId();
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime now = LocalDateTime.now();
    deleteInvoiceAttachments(invoice, now);
    invoice.setDeleted(true);
    invoice.setDeletedAt(now);
    invoice.setExpenseItemId(null);
    invoice.setAttachmentId(null);
    invoice.setSupportAttachmentId(null);
    invoice.setUpdatedBy(userInfo.userId());
    invoice.setUpdatedAt(now);
    invoiceRepository.save(invoice);
    recalculateOrRemoveExpenseItem(reimbursementId, affectedExpenseItemId, userInfo.userId(), now);
    updateExpenseSummary(r);
    r.setUpdatedAt(now);
    reimbursementRepository.save(r);
    return findReimbursementById(reimbursementId);
  }

  private void deleteInvoiceAttachments(Invoice invoice, LocalDateTime now) {
    Set<Long> attachmentIds = java.util.stream.Stream.of(invoice.getAttachmentId(), invoice.getSupportAttachmentId())
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    for (Long attachmentId : attachmentIds) {
      attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
        attachment.setDeleted(true);
        attachment.setDeletedAt(now);
        attachmentRepository.save(attachment);
      });
    }
  }

  @Transactional
  public ReimbursementDTO uploadInvoiceSupportAttachment(Long reimbursementId, Long invoiceId, MultipartFile file) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿、已退回或已撤回状态可上传发票佐证文件");
    }
    Invoice invoice = invoiceRepository.findById(invoiceId)
      .filter(item -> reimbursementId.equals(item.getReimbursementId()))
      .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    validateSupportAttachmentFile(file);
    if (invoice.getSupportAttachmentId() != null) {
      deleteAttachmentRecord(invoice.getSupportAttachmentId());
      invoice.setSupportAttachmentId(null);
    }
    Attachment attachment = saveSupportAttachment(file, reimbursementId);
    invoice.setSupportAttachmentId(attachment.getId());
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    invoice.setUpdatedBy(userInfo.userId());
    invoice.setUpdatedAt(LocalDateTime.now());
    invoiceRepository.save(invoice);
    Path supportPath = uploadDir.resolve(storedFileName(attachment));
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        ocrService.recognizeInvoiceSupportAttachmentAsync(attachment.getId(), supportPath);
      }
    });
    return findReimbursementById(reimbursementId);
  }

  @Transactional
  public ReimbursementDTO deleteInvoiceSupportAttachment(Long reimbursementId, Long invoiceId) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿、已退回或已撤回状态可删除发票佐证文件");
    }
    Invoice invoice = invoiceRepository.findById(invoiceId)
      .filter(item -> reimbursementId.equals(item.getReimbursementId()))
      .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (invoice.getSupportAttachmentId() != null) {
      deleteAttachmentRecord(invoice.getSupportAttachmentId());
      invoice.setSupportAttachmentId(null);
      invoice.setStartPlace(null);
      invoice.setEndPlace(null);
      UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
      invoice.setUpdatedBy(userInfo.userId());
      invoice.setUpdatedAt(LocalDateTime.now());
      invoiceRepository.save(invoice);
      ocrService.regroupExpenseItemsByInvoiceTrips(reimbursementId, userInfo.userId());
    }
    return findReimbursementById(reimbursementId);
  }

  private void validateSupportAttachmentFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(PARAM_ERROR, "文件不能为空");
    }
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new BusinessException(PARAM_ERROR, "文件大小不能超过10MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
      throw new BusinessException(PARAM_ERROR, "仅支持jpg/png/gif/webp/pdf格式");
    }
  }

  private Attachment saveSupportAttachment(MultipartFile file, Long reimbursementId) {
    try {
      Files.createDirectories(uploadDir);
      String originalName = file.getOriginalFilename();
      String ext = "";
      if (originalName != null && originalName.contains(".")) {
        ext = originalName.substring(originalName.lastIndexOf("."));
      }
      String storedName = UUID.randomUUID() + ext;
      Path targetPath = uploadDir.resolve(storedName);
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

      UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
      Attachment attachment = new Attachment();
      attachment.setReimbursementId(reimbursementId);
      attachment.setFileName(originalName);
      attachment.setFileSize(file.getSize());
      attachment.setFileType(file.getContentType());
      attachment.setFileUrl("/attachments/" + storedName + "/download");
      attachment.setUploadedBy(userInfo.userId());
      attachment.setCreatedAt(LocalDateTime.now());
      attachment.setDeleted(false);
      attachment.setOcrStatus("pending");
      return attachmentRepository.save(attachment);
    } catch (IOException e) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "文件上传失败: " + e.getMessage());
    }
  }

  private void deleteAttachmentRecord(Long attachmentId) {
    attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
      attachment.setDeleted(true);
      attachment.setDeletedAt(LocalDateTime.now());
      attachmentRepository.save(attachment);
    });
  }

  private String storedFileName(Attachment attachment) {
    String fileUrl = attachment.getFileUrl();
    int downloadSuffixIndex = fileUrl.lastIndexOf("/download");
    int fileNameStartIndex = fileUrl.lastIndexOf("/", downloadSuffixIndex - 1) + 1;
    return fileUrl.substring(fileNameStartIndex, downloadSuffixIndex);
  }

  public ReimbursementDTO submitReimbursement(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "当前状态不可提交");
    }
    if (r.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(PARAM_ERROR, "报销金额必须大于0");
    }
    r.setStatus("pending");
    r.setUpdatedAt(LocalDateTime.now());
    reimbursementRepository.save(r);

    // 写入审批日志
    writeApprovalLog(r.getId(), "submit", null);

    return findReimbursementById(id);
  }

  public ReimbursementDTO approveReimbursement(Long id, String comment) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"pending".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅待审批状态可通过");
    }
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime now = LocalDateTime.now();

    r.setStatus("approved");
    r.setApproverId(userInfo.userId());
    r.setApproverName(userInfo.username());
    r.setApproveComment(comment);
    r.setApproveTime(now);
    r.setUpdatedAt(now);
    reimbursementRepository.save(r);

    writeApprovalLog(r.getId(), "approve", comment);

    return findReimbursementById(id);
  }

  public ReimbursementDTO rejectReimbursement(Long id, String comment) {
    if (comment == null || comment.isBlank()) {
      throw new BusinessException(PARAM_ERROR, "退回原因必填");
    }
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"pending".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅待审批状态可退回");
    }
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime now = LocalDateTime.now();

    r.setStatus("rejected");
    r.setApproverId(userInfo.userId());
    r.setApproverName(userInfo.username());
    r.setApproveComment(comment);
    r.setApproveTime(now);
    r.setUpdatedAt(now);
    reimbursementRepository.save(r);

    writeApprovalLog(r.getId(), "reject", comment);

    return findReimbursementById(id);
  }

  public ReimbursementDTO recallReimbursement(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"pending".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅待审批状态可撤回");
    }
    r.setStatus("recalled");
    r.setUpdatedAt(LocalDateTime.now());
    reimbursementRepository.save(r);

    writeApprovalLog(r.getId(), "recall", null);

    return findReimbursementById(id);
  }

  private void writeApprovalLog(Long reimbursementId, String action, String comment) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    ApprovalLog log = new ApprovalLog();
    log.setReimbursementId(reimbursementId);
    log.setAction(action);
    log.setOperatorId(userInfo.userId());
    log.setOperatorName(userInfo.username());
    log.setComment(comment);
    log.setCreatedAt(LocalDateTime.now());
    approvalLogRepository.save(log);
  }

  private boolean isApplicantEditableStatus(String status) {
    return "draft".equals(status) || "rejected".equals(status) || "recalled".equals(status);
  }

  // ========== InvoiceTemp（临时发票）相关方法 ==========

  /**
   * 创建临时发票记录（OCR 识别前）
   */
  public InvoiceTemp createInvoiceTemp(Long reimbursementId, String imagePath, Long userId) {
    LocalDateTime now = LocalDateTime.now();
    InvoiceTemp t = new InvoiceTemp();
    t.setReimbursementId(reimbursementId);
    t.setImagePath(imagePath);
    t.setStatus("pending");
    t.setCreatedBy(userId);
    t.setCreatedAt(now);
    t.setUpdatedBy(userId);
    t.setUpdatedAt(now);
    return invoiceTempRepository.save(t);
  }

  /**
   * 更新临时发票的 OCR 识别结果
   */
  public InvoiceTemp updateInvoiceTempOcrResult(Long tempId, String ocrRawJson,
                                                 String invoiceNo, String invoiceCode,
                                                 LocalDateTime invoiceDate,
                                                 String buyerName, String sellerName,
                                                 String buyerTaxId, String sellerTaxId,
                                                 BigDecimal totalAmount,
                                                 String invoiceType, String invoiceStatus,
                                                 Long userId) {
    InvoiceTemp t = invoiceTempRepository.findById(tempId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    t.setOcrRawJson(ocrRawJson);
    t.setInvoiceNo(invoiceNo);
    t.setInvoiceCode(invoiceCode);
    t.setInvoiceDate(invoiceDate);
    t.setBuyerName(buyerName);
    t.setSellerName(sellerName);
    t.setBuyerTaxId(buyerTaxId);
    t.setSellerTaxId(sellerTaxId);
    t.setTotalAmount(totalAmount);
    t.setInvoiceType(invoiceType);
    t.setInvoiceStatus(invoiceStatus);
    t.setUpdatedBy(userId);
    t.setUpdatedAt(LocalDateTime.now());
    return invoiceTempRepository.save(t);
  }

  /**
   * 用户确认临时发票，将数据同步到正式报销单
   */
  public ReimbursementDTO confirmInvoiceTemp(Long tempId) {
    InvoiceTemp t = invoiceTempRepository.findById(tempId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"pending".equals(t.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅待确认状态的临时发票可确认");
    }

    Reimbursement r = reimbursementRepository.findById(t.getReimbursementId())
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));

    // 将临时发票数据同步到正式报销单
    r.setInvoiceNo(t.getInvoiceNo());
    r.setInvoiceCode(t.getInvoiceCode());
    r.setInvoiceDate(t.getInvoiceDate());
    r.setBuyerName(t.getBuyerName());
    r.setSellerName(t.getSellerName());
    r.setBuyerTaxId(t.getBuyerTaxId());
    r.setSellerTaxId(t.getSellerTaxId());
    r.setInvoiceType(t.getInvoiceType());
    r.setInvoiceStatus(t.getInvoiceStatus());
    if (t.getTotalAmount() != null) {
      r.setAmount(t.getTotalAmount());
    }
    // 同步 OCR 原始 JSON
    if (t.getOcrRawJson() != null) {
      r.setOcrRawJson(t.getOcrRawJson());
    }
    r.setUpdatedAt(LocalDateTime.now());
    reimbursementRepository.save(r);

    // 标记临时发票为已确认
    t.setStatus("confirmed");
    t.setUpdatedAt(LocalDateTime.now());
    invoiceTempRepository.save(t);

    // 废弃同一报销单下其他待确认的临时发票
    List<InvoiceTemp> others = invoiceTempRepository
      .findByReimbursementIdAndStatusOrderByCreatedAtDesc(r.getId(), "pending");
    for (InvoiceTemp other : others) {
      other.setStatus("discarded");
      other.setUpdatedAt(LocalDateTime.now());
      invoiceTempRepository.save(other);
    }

    return findReimbursementById(r.getId());
  }

  /**
   * 用户不认可，废弃临时发票
   */
  public void discardInvoiceTemp(Long tempId) {
    InvoiceTemp t = invoiceTempRepository.findById(tempId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"pending".equals(t.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅待确认状态的临时发票可废弃");
    }
    t.setStatus("discarded");
    t.setUpdatedAt(LocalDateTime.now());
    invoiceTempRepository.save(t);
  }

  /**
   * 查询报销单下的临时发票列表
   */
  public List<ReimbursementDTO.InvoiceTempDTO> findInvoiceTempsByReimbursementId(Long reimbursementId) {
    List<InvoiceTemp> list = invoiceTempRepository.findByReimbursementIdOrderByCreatedAtDesc(reimbursementId);
    return list.stream().map(ReimbursementDTO.InvoiceTempDTO::new).toList();
  }

  public ReimbursementDTO saveExpenseItems(Long reimbursementId, List<ExpenseItemRequest> items) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!isApplicantEditableStatus(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "当前状态不可编辑费用明细");
    }
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime now = LocalDateTime.now();
    deduplicateInvoicesOnSave(reimbursementId);
    List<Invoice> reimbursementInvoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId);
    Map<Long, Long> existingInvoiceCounts = reimbursementInvoices.stream()
      .map(Invoice::getExpenseItemId)
      .filter(Objects::nonNull)
      .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
    for (Invoice invoice : reimbursementInvoices) {
      invoice.setExpenseItemId(null);
      invoice.setUpdatedBy(userInfo.userId());
      invoice.setUpdatedAt(now);
      invoiceRepository.save(invoice);
    }
    expenseItemRepository.deleteByReimbursementId(reimbursementId);
    items = mergeSameDateExpenseItems(items);
    if (items != null) {
      int index = 1;
      for (ExpenseItemRequest req : items) {
        if (req == null || req.expenseType() == null || req.expenseType().isBlank()) continue;
        BigDecimal invoiceTotal = sumInvoices(reimbursementInvoices, req.invoiceIds());
        BigDecimal requestedAmount = req.amount() == null ? BigDecimal.ZERO : req.amount();
        if (shouldDropEmptyExpenseItem(req, existingInvoiceCounts, invoiceTotal, requestedAmount)) continue;
        ReimbursementExpenseItem item = new ReimbursementExpenseItem();
        item.setReimbursementId(reimbursementId);
        item.setInvoiceId(req.invoiceId());
        item.setExpenseDate(req.expenseDate());
        item.setStartPlace(req.startPlace());
        item.setEndPlace(req.endPlace());
        item.setKilometers(req.kilometers());
        item.setExpenseType(req.expenseType());
        item.setOaExpenseName(oaExpenseName(req.expenseType()));
        item.setAmount(invoiceTotal.compareTo(BigDecimal.ZERO) > 0 ? invoiceTotal : requestedAmount);
        item.setProjectCostCategory(req.projectCostCategory());
        item.setCustomerName(req.customerName());
        item.setRemark(req.remark());
        item.setDescription(req.description());
        item.setSource(req.source() == null || req.source().isBlank() ? "manual" : req.source());
        item.setSortOrder(index++);
        item.setCreatedBy(userInfo.userId());
        item.setCreatedAt(now);
        item.setUpdatedBy(userInfo.userId());
        item.setUpdatedAt(now);
        ReimbursementExpenseItem saved = expenseItemRepository.save(item);
        if (req.invoiceIds() != null) {
          for (Long invoiceId : req.invoiceIds()) {
            invoiceRepository.findById(invoiceId)
              .filter(invoice -> reimbursementId.equals(invoice.getReimbursementId()))
              .ifPresent(invoice -> {
                invoice.setExpenseItemId(saved.getId());
                invoice.setUpdatedBy(userInfo.userId());
                invoice.setUpdatedAt(now);
                invoiceRepository.save(invoice);
              });
          }
        }
      }
    }
    updateExpenseSummary(r);
    r.setUpdatedAt(now);
    reimbursementRepository.save(r);
    return findReimbursementById(reimbursementId);
  }

  private List<ExpenseItemRequest> mergeSameDateExpenseItems(List<ExpenseItemRequest> items) {
    if (items == null || items.isEmpty()) return items;
    Map<String, ExpenseItemRequest> merged = new LinkedHashMap<>();
    for (ExpenseItemRequest item : items) {
      if (item == null || item.expenseType() == null || item.expenseType().isBlank()) continue;
      String key = expenseItemMergeKey(item);
      if (key == null) {
        merged.put("single|" + merged.size(), item);
        continue;
      }
      ExpenseItemRequest existing = merged.get(key);
      if (existing == null) {
        merged.put(key, item);
      } else {
        merged.put(key, mergeExpenseItem(existing, item));
      }
    }
    return new ArrayList<>(merged.values());
  }

  private String expenseItemMergeKey(ExpenseItemRequest item) {
    if (item.expenseDate() == null) return null;
    String date = item.expenseDate() == null ? "" : item.expenseDate().toLocalDate().toString();
    return item.expenseType() + "|" + date;
  }

  private ExpenseItemRequest mergeExpenseItem(ExpenseItemRequest left, ExpenseItemRequest right) {
    List<Long> invoiceIds = new ArrayList<>();
    addInvoiceIds(invoiceIds, left.invoiceIds());
    addInvoiceIds(invoiceIds, right.invoiceIds());
    BigDecimal amount = left.amount() == null ? BigDecimal.ZERO : left.amount();
    if (right.amount() != null) amount = amount.add(right.amount());
    return new ExpenseItemRequest(
      left.id(),
      null,
      invoiceIds,
      firstNonNull(left.expenseDate(), right.expenseDate()),
      firstNonBlank(left.startPlace(), right.startPlace()),
      firstNonBlank(left.endPlace(), right.endPlace()),
      firstNonNull(left.kilometers(), right.kilometers()),
      left.expenseType(),
      amount,
      firstNonBlank(left.projectCostCategory(), right.projectCostCategory()),
      firstNonBlank(left.customerName(), right.customerName()),
      firstNonBlank(left.remark(), right.remark()),
      firstNonBlank(left.description(), right.description()),
      firstNonBlank(left.source(), right.source())
    );
  }

  private void addInvoiceIds(List<Long> target, List<Long> source) {
    if (source == null) return;
    for (Long id : source) {
      if (id != null && !target.contains(id)) target.add(id);
    }
  }

  private <T> T firstNonNull(T left, T right) {
    return left != null ? left : right;
  }

  public byte[] exportElectronicInvoiceRegister(Long reimbursementId) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId);
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("电子发票登记表");
      createTitle(sheet, workbook, "电子发票登记表", 0, 10);
      String[] headers = {"发票类型", "发票号码", "开票日期", "发票金额(不含税)", "发票税额", "发票税率", "价税合计", "报销人", "提交日期", "销方名称（简写）", "发票内容（简写）"};
      writeHeader(sheet, workbook, 2, headers);
      int rowIndex = 3;
      for (Invoice invoice : invoices) {
        Row row = sheet.createRow(rowIndex++);
        setCell(row, 0, invoiceTypeLabel(invoice.getInvoiceType()));
        setCell(row, 1, invoice.getInvoiceNo());
        setCell(row, 2, formatDate(invoice.getInvoiceDate()));
        setCell(row, 3, invoice.getAmountBeforeTax());
        setCell(row, 4, invoice.getTaxAmount());
        setCell(row, 5, invoice.getTaxRate());
        setCell(row, 6, invoice.getTotalAmount());
        setCell(row, 7, r.getApplicantName());
        setCell(row, 8, formatDate(invoice.getSubmitDate() != null ? invoice.getSubmitDate() : LocalDateTime.now()));
        setCell(row, 9, firstNonBlank(invoice.getSellerShortName(), invoice.getSellerName()));
        setCell(row, 10, firstNonBlank(invoice.getInvoiceContentShortName(), invoice.getRemarks(), oaExpenseNameByInvoice(invoice)));
      }
      applyReadableLayout(sheet, headers.length, 2);
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "导出电子发票登记表失败");
    }
  }

  public byte[] exportExpenseDetails(Long reimbursementId) {
    Reimbursement r = reimbursementRepository.findById(reimbursementId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    List<ReimbursementExpenseItem> items = expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursementId);
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("报销费用明细表");
      createTitle(sheet, workbook, "报销费用明细表", 0, 15);
      Row applicant = sheet.createRow(2);
      setCell(applicant, 0, "报销人：");
      setCell(applicant, 1, r.getApplicantName());
      String[] headers = {"时间", "始发", "终点", "公里数", "加油金额", "高速费", "高（城际/动车）铁费", "地铁费", "网约车费", "顺风车费", "货拉拉费", "停车费", "餐费", "合计", "备注", "说明"};
      writeHeader(sheet, workbook, 4, headers);
      int rowIndex = 5;
      BigDecimal total = BigDecimal.ZERO;
      for (ReimbursementExpenseItem item : items) {
        Row row = sheet.createRow(rowIndex++);
        setCell(row, 0, formatDate(item.getExpenseDate()));
        setCell(row, 1, item.getStartPlace());
        setCell(row, 2, item.getEndPlace());
        setCell(row, 3, item.getKilometers());
        int amountColumn = expenseAmountColumn(item.getExpenseType());
        if (amountColumn >= 0) setCell(row, amountColumn, item.getAmount());
        setCell(row, 13, item.getAmount());
        setCell(row, 14, item.getRemark());
        setCell(row, 15, item.getDescription());
        if (item.getAmount() != null) total = total.add(item.getAmount());
      }
      Row totalRow = sheet.createRow(rowIndex);
      setCell(totalRow, 0, "合计：");
      setCell(totalRow, 13, total);
      applyReadableLayout(sheet, headers.length, 4);
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "导出报销费用明细表失败");
    }
  }

  public byte[] exportBatchOaExcels(List<Long> reimbursementIds) {
    if (reimbursementIds == null || reimbursementIds.isEmpty()) {
      throw new BusinessException(PARAM_ERROR, "请选择需要导出的报销单");
    }
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      int index = 1;
      for (Long reimbursementId : reimbursementIds) {
        Reimbursement r = reimbursementRepository.findById(reimbursementId)
          .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
        String prefix = String.format("%03d-报销单%s-%s-", index++, r.getId(), safeFilePart(r.getApplicantName()));
        addZipEntry(zip, prefix + "电子发票登记表.xlsx", exportElectronicInvoiceRegister(reimbursementId));
        addZipEntry(zip, prefix + "报销费用明细表.xlsx", exportExpenseDetails(reimbursementId));
      }
      zip.finish();
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "批量导出OA报销表失败");
    }
  }

  private void addZipEntry(ZipOutputStream zip, String filename, byte[] content) throws java.io.IOException {
    zip.putNextEntry(new ZipEntry(filename));
    zip.write(content);
    zip.closeEntry();
  }

  private String safeFilePart(String value) {
    if (value == null || value.isBlank()) return "未命名";
    return value.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private void updateExpenseSummary(Reimbursement r) {
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(r.getId());
    removeOrphanOcrExpenseItems(r.getId(), invoices);
    BigDecimal invoiceTotal = invoices.stream()
      .map(Invoice::getTotalAmount)
      .filter(Objects::nonNull)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    r.setInvoiceCount(invoices.size());
    r.setInvoiceTotalAmount(invoiceTotal);
    r.setAmount(invoiceTotal);
    syncVoucherOcrDescription(r, invoices);
  }

  private void deduplicateInvoicesOnSave(Long reimbursementId) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    Long userId = userInfo == null ? null : userInfo.userId();
    LocalDateTime now = LocalDateTime.now();
    Map<String, Invoice> seen = new LinkedHashMap<>();
    List<Long> affectedExpenseItemIds = new ArrayList<>();
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId);
    for (Invoice invoice : invoices) {
      String key = invoiceDeduplicateKey(invoice);
      if (key == null) continue;
      Invoice existing = seen.get(key);
      if (existing == null) {
        seen.put(key, invoice);
        continue;
      }
      affectedExpenseItemIds.add(invoice.getExpenseItemId());
      deleteInvoiceAttachments(invoice, now);
      invoice.setDeleted(true);
      invoice.setDeletedAt(now);
      invoice.setAttachmentId(null);
      invoice.setSupportAttachmentId(null);
      invoice.setExpenseItemId(null);
      invoice.setUpdatedBy(userId);
      invoice.setUpdatedAt(now);
      invoiceRepository.save(invoice);
    }
    affectedExpenseItemIds.stream()
      .filter(Objects::nonNull)
      .distinct()
      .forEach(expenseItemId -> recalculateOrRemoveExpenseItem(reimbursementId, expenseItemId, userId, now));
  }

  private String invoiceDeduplicateKey(Invoice invoice) {
    if (invoice == null) return null;
    if (invoice.getInvoiceNo() != null && !invoice.getInvoiceNo().isBlank()) {
      return "no|" + invoice.getInvoiceNo().trim();
    }
    if (invoice.getInvoiceCode() != null && !invoice.getInvoiceCode().isBlank()
      && invoice.getInvoiceDate() != null && invoice.getTotalAmount() != null) {
      return "fallback|" + invoice.getInvoiceCode().trim()
        + "|" + invoice.getInvoiceDate().toLocalDate()
        + "|" + invoice.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP);
    }
    return null;
  }

  private void syncVoucherOcrDescription(Reimbursement r, List<Invoice> activeInvoices) {
    Invoice latestOcrInvoice = activeInvoices.stream()
      .filter(invoice -> invoice.getOcrRawJson() != null && !invoice.getOcrRawJson().isBlank())
      .reduce((first, second) -> second)
      .orElse(null);
    if (latestOcrInvoice == null) {
      r.setDescription("");
      r.setOcrRawJson("");
      return;
    }
    r.setOcrRawJson(latestOcrInvoice.getOcrRawJson());
    r.setDescription(stripSupplementaryChars(extractOcrText(latestOcrInvoice.getOcrRawJson())));
  }

  private String extractOcrText(String ocrRawJson) {
    if (ocrRawJson == null || ocrRawJson.isBlank()) return "";
    try {
      JsonNode root = objectMapper.readTree(ocrRawJson);
      JsonNode data = unwrapOcrData(root);
      if (data.has("ocr_text")) return data.get("ocr_text").asText("");
    } catch (Exception ignored) {
    }
    return "";
  }

  private JsonNode unwrapOcrData(JsonNode root) {
    JsonNode content = null;
    if (root.has("result") && root.get("result").has("content")) {
      content = root.get("result").get("content");
    } else if (root.has("content")) {
      content = root.get("content");
    }
    if (content != null && content.isArray() && !content.isEmpty() && content.get(0).has("text")) {
      try {
        return objectMapper.readTree(content.get(0).get("text").asText());
      } catch (Exception ignored) {
        return root;
      }
    }
    return root;
  }

  private String stripSupplementaryChars(String value) {
    if (value == null) return null;
    return value.codePoints()
      .filter(codePoint -> codePoint <= Character.MAX_VALUE)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString();
  }

  private void removeOrphanOcrExpenseItems(Long reimbursementId, List<Invoice> invoices) {
    List<Long> boundExpenseItemIds = invoices.stream()
      .map(Invoice::getExpenseItemId)
      .filter(Objects::nonNull)
      .toList();
    expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursementId).stream()
      .filter(item -> "ocr".equals(item.getSource()))
      .filter(item -> item.getId() != null && !boundExpenseItemIds.contains(item.getId()))
      .forEach(expenseItemRepository::delete);
  }

  private void recalculateOrRemoveExpenseItem(Long reimbursementId, Long expenseItemId, Long userId, LocalDateTime now) {
    if (expenseItemId == null) return;
    expenseItemRepository.findById(expenseItemId)
      .filter(item -> reimbursementId.equals(item.getReimbursementId()))
      .ifPresent(item -> {
        List<Invoice> boundInvoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId).stream()
          .filter(invoice -> expenseItemId.equals(invoice.getExpenseItemId()))
          .toList();
        if (boundInvoices.isEmpty()) {
          expenseItemRepository.delete(item);
          return;
        }
        BigDecimal invoiceTotal = boundInvoices.stream()
          .map(Invoice::getTotalAmount)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        item.setAmount(invoiceTotal);
        item.setUpdatedBy(userId);
        item.setUpdatedAt(now);
        expenseItemRepository.save(item);
      });
  }

  private boolean shouldDropEmptyExpenseItem(ExpenseItemRequest req, Map<Long, Long> existingInvoiceCounts,
                                             BigDecimal invoiceTotal, BigDecimal requestedAmount) {
    if (req.id() == null) return false;
    boolean hadInvoices = existingInvoiceCounts.getOrDefault(req.id(), 0L) > 0;
    boolean hasSelectedInvoices = req.invoiceIds() != null && !req.invoiceIds().isEmpty();
    return hadInvoices && !hasSelectedInvoices
      && invoiceTotal.compareTo(BigDecimal.ZERO) == 0
      && requestedAmount.compareTo(BigDecimal.ZERO) == 0;
  }

  private BigDecimal sumInvoices(List<Invoice> invoices, List<Long> invoiceIds) {
    if (invoiceIds == null || invoiceIds.isEmpty()) return BigDecimal.ZERO;
    return invoices.stream()
      .filter(invoice -> invoiceIds.contains(invoice.getId()))
      .map(Invoice::getTotalAmount)
      .filter(Objects::nonNull)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void createTitle(Sheet sheet, Workbook workbook, String title, int firstCol, int lastCol) {
    Row row = sheet.createRow(0);
    row.setHeightInPoints(26);
    setCell(row, firstCol, title);
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    style.setFont(font);
    row.getCell(firstCol).setCellStyle(style);
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, firstCol, lastCol));
  }

  private void writeHeader(Sheet sheet, Workbook workbook, int rowIndex, String[] headers) {
    Row row = sheet.createRow(rowIndex);
    row.setHeightInPoints(24);
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setWrapText(true);
    for (int i = 0; i < headers.length; i++) {
      setCell(row, i, headers[i]);
      row.getCell(i).setCellStyle(style);
    }
  }

  private void setCell(Row row, int index, Object value) {
    var cell = row.createCell(index);
    if (value == null) cell.setBlank();
    else if (value instanceof BigDecimal amount) cell.setCellValue(amount.doubleValue());
    else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
    else cell.setCellValue(String.valueOf(value));
  }

  private void autosize(Sheet sheet, int cols) {
    for (int i = 0; i < cols; i++) {
      sheet.autoSizeColumn(i);
      int minWidth = switch (i) {
        case 0 -> 3600;
        case 1, 2 -> 5200;
        case 6 -> 6200;
        case 14 -> 5200;
        case 15 -> 7600;
        default -> 3600;
      };
      int width = Math.max(sheet.getColumnWidth(i) + 512, minWidth);
      sheet.setColumnWidth(i, Math.min(width, 10000));
    }
  }

  private void applyReadableLayout(Sheet sheet, int cols, int headerRowIndex) {
    autosize(sheet, cols);
    sheet.createFreezePane(0, headerRowIndex + 1);
    for (Row row : sheet) {
      if (row.getRowNum() > headerRowIndex) {
        row.setHeightInPoints(28);
      }
      row.forEach(cell -> {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.cloneStyleFrom(cell.getCellStyle());
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
      });
    }
  }

  private String formatDate(LocalDateTime date) {
    return date == null ? "" : date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
  }

  private int expenseAmountColumn(String expenseType) {
    return switch (expenseType == null ? "" : expenseType) {
      case "fuel" -> 4;
      case "toll" -> 5;
      case "rail" -> 6;
      case "metro" -> 7;
      case "ride_hailing" -> 8;
      case "carpool" -> 9;
      case "freight" -> 10;
      case "parking" -> 11;
      case "meal" -> 12;
      default -> -1;
    };
  }

  private String oaExpenseName(String expenseType) {
    return switch (expenseType == null ? "" : expenseType) {
      case "fuel" -> "加油费";
      case "toll" -> "高速过路费";
      case "rail" -> "高铁/动车/火车费";
      case "metro" -> "地铁费";
      case "ride_hailing" -> "网约车费";
      case "carpool" -> "顺风车费";
      case "freight" -> "货拉拉货运费";
      case "parking" -> "停车费";
      case "meal" -> "餐费";
      default -> "其他";
    };
  }

  private String invoiceTypeLabel(String invoiceType) {
    if (invoiceType == null || invoiceType.isBlank()) return "普票";
    return switch (invoiceType) {
      case "special" -> "专票";
      case "normal", "electronic" -> "普票";
      default -> invoiceType;
    };
  }

  private String oaExpenseNameByInvoice(Invoice invoice) {
    if (invoice.getExpenseItemId() == null) return "";
    return expenseItemRepository.findById(invoice.getExpenseItemId())
      .map(ReimbursementExpenseItem::getOaExpenseName)
      .orElse("");
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  public record ExpenseItemRequest(
    Long id,
    Long invoiceId,
    List<Long> invoiceIds,
    LocalDateTime expenseDate,
    String startPlace,
    String endPlace,
    BigDecimal kilometers,
    String expenseType,
    BigDecimal amount,
    String projectCostCategory,
    String customerName,
    String remark,
    String description,
    String source
  ) {}
}
