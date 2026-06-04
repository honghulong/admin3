package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.InvoiceTemp;

import java.util.List;

public interface InvoiceTempRepository extends JpaRepository<InvoiceTemp, Long> {

  List<InvoiceTemp> findByReimbursementIdOrderByCreatedAtDesc(Long reimbursementId);

  List<InvoiceTemp> findByReimbursementIdAndStatusOrderByCreatedAtDesc(Long reimbursementId, String status);
}
