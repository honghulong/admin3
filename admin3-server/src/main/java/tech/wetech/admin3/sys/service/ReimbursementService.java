package tech.wetech.admin3.sys.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.CommonResultStatus;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.InvoiceTemp;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.ApprovalLogRepository;
import tech.wetech.admin3.sys.repository.InvoiceTempRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static tech.wetech.admin3.common.CommonResultStatus.PARAM_ERROR;
import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@Service
@Transactional
public class ReimbursementService {

  private final ReimbursementRepository reimbursementRepository;
  private final AttachmentRepository attachmentRepository;
  private final ApprovalLogRepository approvalLogRepository;
  private final InvoiceTempRepository invoiceTempRepository;

  public ReimbursementService(ReimbursementRepository reimbursementRepository,
                              AttachmentRepository attachmentRepository,
                              ApprovalLogRepository approvalLogRepository,
                              InvoiceTempRepository invoiceTempRepository) {
    this.reimbursementRepository = reimbursementRepository;
    this.attachmentRepository = attachmentRepository;
    this.approvalLogRepository = approvalLogRepository;
    this.invoiceTempRepository = invoiceTempRepository;
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
    List<Attachment> attachments = attachmentRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    List<ApprovalLog> logs = approvalLogRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    List<InvoiceTemp> invoiceTemps = invoiceTempRepository.findByReimbursementIdOrderByCreatedAtDesc(id);
    return ReimbursementDTO.of(r, attachments, logs, invoiceTemps);
  }

  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description, List<Long> attachmentIds) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    return createReimbursement(title, category, amount, description, attachmentIds,
      userInfo.userId(), userInfo.username(), null, null, null,
      null, null, null, null, null, null, null);
  }

  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description,
                                              String invoiceNo, String invoiceCode, String invoiceDate,
                                              String buyerName, String sellerName,
                                              String buyerTaxId, String sellerTaxId,
                                              String invoiceType, String invoiceStatus,
                                              List<Long> attachmentIds) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime date = invoiceDate != null ? LocalDateTime.parse(invoiceDate) : null;
    return createReimbursement(title, category, amount, description, attachmentIds,
      userInfo.userId(), userInfo.username(), invoiceNo, invoiceCode, date,
      buyerName, sellerName, buyerTaxId, sellerTaxId, invoiceType, invoiceStatus,
      null);
  }

  /**
   * 创建报销单（支持 MCP 无 Session 场景，直接传入用户信息）
   */
  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description, List<Long> attachmentIds,
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
    return updateReimbursement(id, title, category, amount, description,
      null, null, null, null, null, null, null, null, null, attachmentIds);
  }

  public ReimbursementDTO updateReimbursement(Long id, String title, String category, BigDecimal amount,
                                              String description,
                                              String invoiceNo, String invoiceCode, String invoiceDate,
                                              String buyerName, String sellerName,
                                              String buyerTaxId, String sellerTaxId,
                                              String invoiceType, String invoiceStatus,
                                              List<Long> attachmentIds) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"draft".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿状态可编辑");
    }
    r.setTitle(title);
    r.setCategory(category);
    r.setAmount(amount);
    r.setDescription(description);
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
      // 解除旧关联
      List<Attachment> oldAttachments = attachmentRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
      for (Attachment a : oldAttachments) {
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

    return findReimbursementById(id);
  }

  public void deleteReimbursement(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"draft".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿状态可删除");
    }
    // 级联删除附件和审批日志
    attachmentRepository.deleteByReimbursementId(id);
    List<ApprovalLog> logs = approvalLogRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    approvalLogRepository.deleteAll(logs);
    reimbursementRepository.delete(r);
  }

  public ReimbursementDTO submitReimbursement(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"draft".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿状态可提交");
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
}
