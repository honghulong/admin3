package tech.wetech.admin3.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.wetech.admin3.sys.model.OcrEngineConfig;

import java.util.List;
import java.util.Optional;

public interface OcrEngineConfigRepository extends JpaRepository<OcrEngineConfig, Long> {

  Optional<OcrEngineConfig> findFirstByEnabledTrueAndIsDefaultTrueOrderByUpdatedAtDesc();

  Optional<OcrEngineConfig> findFirstByProviderTypeAndEnabledTrueOrderByUpdatedAtDesc(String providerType);

  List<OcrEngineConfig> findByEnabledTrueOrderByIsDefaultDescUpdatedAtDesc();
}
