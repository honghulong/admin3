package tech.wetech.admin3.sys.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.wetech.admin3.sys.model.Reimbursement;

import java.time.LocalDateTime;
import java.util.List;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {

  Page<Reimbursement> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

  Page<Reimbursement> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

  Page<Reimbursement> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("SELECT r FROM Reimbursement r WHERE " +
    "(:status IS NULL OR r.status = :status) AND " +
    "(:applicantId IS NULL OR r.applicantId = :applicantId) AND " +
    "(:category IS NULL OR r.category = :category) AND " +
    "(:dateFrom IS NULL OR r.createdAt >= :dateFrom) AND " +
    "(:dateTo IS NULL OR r.createdAt <= :dateTo) AND " +
    "(:keyword IS NULL OR r.title LIKE CONCAT('%', :keyword, '%') OR r.description LIKE CONCAT('%', :keyword, '%')) " +
    "ORDER BY r.createdAt DESC")
  Page<Reimbursement> searchReimbursements(
    @Param("status") String status,
    @Param("applicantId") Long applicantId,
    @Param("category") String category,
    @Param("dateFrom") LocalDateTime dateFrom,
    @Param("dateTo") LocalDateTime dateTo,
    @Param("keyword") String keyword,
    Pageable pageable);

  List<Reimbursement> findByApplicantIdAndStatus(Long applicantId, String status);

  List<Reimbursement> findByApplicantNameOrderByCreatedAtDesc(String applicantName);
}
