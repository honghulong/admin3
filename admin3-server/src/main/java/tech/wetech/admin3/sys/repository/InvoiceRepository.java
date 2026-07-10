package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  List<Invoice> findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(Long reimbursementId);

  List<Invoice> findByReimbursementIdOrderByCreatedAtAsc(Long reimbursementId);

  Optional<Invoice> findByAttachmentIdAndDeletedFalse(Long attachmentId);
}
