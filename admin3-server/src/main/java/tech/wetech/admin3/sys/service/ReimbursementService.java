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
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.ApprovalLogRepository;
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

  public ReimbursementService(ReimbursementRepository reimbursementRepository,
                              AttachmentRepository attachmentRepository,
                              ApprovalLogRepository approvalLogRepository) {
    this.reimbursementRepository = reimbursementRepository;
    this.attachmentRepository = attachmentRepository;
    this.approvalLogRepository = approvalLogRepository;
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

  public ReimbursementDTO findReimbursementById(Long id) {
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    List<Attachment> attachments = attachmentRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    List<ApprovalLog> logs = approvalLogRepository.findByReimbursementIdOrderByCreatedAtAsc(id);
    return ReimbursementDTO.of(r, attachments, logs);
  }

  public ReimbursementDTO createReimbursement(String title, String category, BigDecimal amount,
                                              String description, List<Long> attachmentIds) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    LocalDateTime now = LocalDateTime.now();

    Reimbursement r = new Reimbursement();
    r.setTitle(title);
    r.setCategory(category);
    r.setAmount(amount);
    r.setDescription(description);
    r.setStatus("draft");
    r.setApplicantId(userInfo.userId());
    r.setApplicantName(userInfo.username());
    r.setCreatedAt(now);
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
    Reimbursement r = reimbursementRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    if (!"draft".equals(r.getStatus())) {
      throw new BusinessException(PARAM_ERROR, "仅草稿状态可编辑");
    }
    r.setTitle(title);
    r.setCategory(category);
    r.setAmount(amount);
    r.setDescription(description);
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
}
