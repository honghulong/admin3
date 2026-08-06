package tech.wetech.admin3.sys.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.wetech.admin3.sys.model.IncomingDocument;

public interface IncomingDocumentRepository extends JpaRepository<IncomingDocument, Long> {

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, Long id);

  boolean existsByIdAndDeletedFalse(Long id);

  @Query("""
    select d from IncomingDocument d
    where d.deleted = false
      and (:keyword is null or :keyword = ''
      or lower(d.code) like lower(concat('%', :keyword, '%'))
      or lower(d.title) like lower(concat('%', :keyword, '%'))
      or lower(d.docNo) like lower(concat('%', :keyword, '%'))
      or lower(d.receivedNo) like lower(concat('%', :keyword, '%'))
      or lower(d.incomingUnit) like lower(concat('%', :keyword, '%'))
      or lower(d.carrierNo) like lower(concat('%', :keyword, '%')))
    order by d.createdAt desc
    """)
  Page<IncomingDocument> search(String keyword, Pageable pageable);
}
