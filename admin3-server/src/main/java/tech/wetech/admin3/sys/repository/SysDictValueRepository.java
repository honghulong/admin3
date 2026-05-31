package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.SysDictValue;

import java.util.List;

public interface SysDictValueRepository extends JpaRepository<SysDictValue, Long> {

  List<SysDictValue> findByDictIdOrderBySortOrderAsc(Long dictId);

  void deleteByDictId(Long dictId);
}
