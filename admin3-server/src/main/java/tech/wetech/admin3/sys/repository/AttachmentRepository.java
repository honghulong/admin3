package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.Attachment;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByReimbursementIdOrderByCreatedAtAsc(Long reimbursementId);

  List<Attachment> findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(Long reimbursementId);

  List<Attachment> findByIncomingDocumentIdAndDeletedFalseOrderByCreatedAtAsc(Long incomingDocumentId);

  void deleteByReimbursementId(Long reimbursementId);

  void deleteByIncomingDocumentId(Long incomingDocumentId);

  long countByReimbursementId(Long reimbursementId);

  Attachment findByFileUrlEndingWith(String fileUrlSuffix);

  Attachment findByFileUrlEndingWithAndDeletedFalse(String fileUrlSuffix);
}
