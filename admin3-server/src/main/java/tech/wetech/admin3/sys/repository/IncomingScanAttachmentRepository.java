package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.IncomingScanAttachment;

import java.util.List;

public interface IncomingScanAttachmentRepository extends JpaRepository<IncomingScanAttachment, Long> {

  List<IncomingScanAttachment> findByIncomingDocumentIdAndDeletedFalseOrderByCreatedAtAsc(Long incomingDocumentId);

  List<IncomingScanAttachment> findByIncomingDocumentIdOrderByCreatedAtAsc(Long incomingDocumentId);
}
