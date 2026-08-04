package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.OcrMappingConfig;

import java.util.Optional;

public interface OcrMappingConfigRepository extends JpaRepository<OcrMappingConfig, Long> {

  Optional<OcrMappingConfig> findFirstByModuleAndUserIdOrderByUpdatedAtDesc(String module, Long userId);
}
