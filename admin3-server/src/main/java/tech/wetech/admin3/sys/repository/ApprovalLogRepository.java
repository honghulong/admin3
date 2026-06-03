package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.ApprovalLog;

import java.util.List;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {

  List<ApprovalLog> findByReimbursementIdOrderByCreatedAtAsc(Long reimbursementId);
}
