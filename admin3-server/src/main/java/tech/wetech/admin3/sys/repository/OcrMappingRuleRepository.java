package tech.wetech.admin3.sys.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.wetech.admin3.sys.model.OcrMappingRule;

import java.util.Optional;

public interface OcrMappingRuleRepository extends JpaRepository<OcrMappingRule, Long> {

  boolean existsByNameAndDeletedFalse(String name);

  boolean existsByNameAndIdNotAndDeletedFalse(String name, Long id);

  Optional<OcrMappingRule> findFirstByModuleAndDeletedFalseOrderByUpdatedAtDesc(String module);

  @Query("""
    select r from OcrMappingRule r
    where r.deleted = false
      and (:keyword is null or :keyword = ''
        or lower(r.name) like lower(concat('%', :keyword, '%'))
        or lower(r.module) like lower(concat('%', :keyword, '%'))
        or lower(r.formType) like lower(concat('%', :keyword, '%')))
    order by r.updatedAt desc
    """)
  Page<OcrMappingRule> search(String keyword, Pageable pageable);
}
