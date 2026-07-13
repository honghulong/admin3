package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;

import java.util.List;
import java.util.Optional;

public interface ReimbursementExpenseItemRepository extends JpaRepository<ReimbursementExpenseItem, Long> {

  List<ReimbursementExpenseItem> findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(Long reimbursementId);

  Optional<ReimbursementExpenseItem> findByInvoiceId(Long invoiceId);

  void deleteByReimbursementId(Long reimbursementId);
}
