package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.Attachment;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByReimbursementIdOrderByCreatedAtAsc(Long reimbursementId);

  void deleteByReimbursementId(Long reimbursementId);

  long countByReimbursementId(Long reimbursementId);

  Attachment findByFileUrlEndingWith(String fileUrlSuffix);
}
