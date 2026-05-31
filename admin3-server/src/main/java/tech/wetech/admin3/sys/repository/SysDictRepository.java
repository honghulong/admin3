package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.SysDict;

import java.util.Optional;

public interface SysDictRepository extends JpaRepository<SysDict, Long> {

  Optional<SysDict> findByDictCode(String dictCode);

  boolean existsByDictCode(String dictCode);
}
