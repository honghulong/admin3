package tech.wetech.admin3.sys.service.ocr;

import tech.wetech.admin3.sys.model.OcrEngineConfig;

public interface OcrProvider {

  boolean supports(String providerType);

  String recognize(OcrEngineConfig config, OcrRecognitionRequest request) throws Exception;
}
