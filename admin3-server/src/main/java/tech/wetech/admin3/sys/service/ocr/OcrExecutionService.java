package tech.wetech.admin3.sys.service.ocr;

import org.springframework.stereotype.Service;
import tech.wetech.admin3.sys.model.OcrEngineConfig;
import tech.wetech.admin3.sys.repository.OcrEngineConfigRepository;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OcrExecutionService {

  private static final Logger log = LoggerFactory.getLogger(OcrExecutionService.class);

  private final OcrEngineConfigRepository configRepository;
  private final List<OcrProvider> providers;

  public OcrExecutionService(OcrEngineConfigRepository configRepository, List<OcrProvider> providers) {
    this.configRepository = configRepository;
    this.providers = providers;
  }

  public String recognizeBase64(String imageBase64, String filename) throws Exception {
    OcrRecognitionRequest request = new OcrRecognitionRequest(imageBase64, filename, null);
    OcrEngineConfig config = selectedConfig();
    log.info("OCR routed to provider={}, configId={}, name={}, filename={}",
      config.getProviderType(), config.getId(), config.getName(), filename);
    try {
      return providerFor(config.getProviderType()).recognize(config, request);
    } catch (Exception e) {
      if (Boolean.TRUE.equals(config.getFallbackEnabled()) && !"INTERNAL".equalsIgnoreCase(config.getProviderType())) {
        OcrEngineConfig internal = configRepository.findFirstByProviderTypeAndEnabledTrueOrderByUpdatedAtDesc("INTERNAL")
          .orElseGet(this::internalConfig);
        log.warn("OCR provider {} failed for {}, fallback to INTERNAL: {}",
          config.getProviderType(), filename, e.getMessage());
        return providerFor("INTERNAL").recognize(internal, request);
      }
      throw e;
    }
  }

  public boolean isExternalProviderSelected() {
    return !"INTERNAL".equalsIgnoreCase(selectedConfig().getProviderType());
  }

  public boolean useBboxInvoiceParser() {
    OcrEngineConfig config = selectedConfig();
    return "INTERNAL".equalsIgnoreCase(config.getProviderType())
      && !"PDF_TEXT_REGEX".equalsIgnoreCase(config.getParseStrategy());
  }

  private OcrEngineConfig selectedConfig() {
    return configRepository.findByEnabledTrueOrderByIsDefaultDescUpdatedAtDesc().stream().findFirst()
      .orElseGet(this::internalConfig);
  }

  private OcrProvider providerFor(String providerType) {
    return providers.stream()
      .filter(provider -> provider.supports(providerType))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported OCR provider: " + providerType));
  }

  private OcrEngineConfig internalConfig() {
    OcrEngineConfig config = new OcrEngineConfig();
    config.setProviderType("INTERNAL");
    config.setBaseUrl("http://127.0.0.1:8769");
    config.setParseStrategy("BBOX_REGION");
    config.setFallbackEnabled(false);
    return config;
  }
}
