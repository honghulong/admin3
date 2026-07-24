package tech.wetech.admin3.sys.service.ocr;

public record OcrRecognitionRequest(
  String imageBase64,
  String filename,
  String prompt
) {
}
