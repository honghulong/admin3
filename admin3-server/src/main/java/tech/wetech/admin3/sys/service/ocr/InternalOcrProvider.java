package tech.wetech.admin3.sys.service.ocr;

import org.springframework.stereotype.Component;
import tech.wetech.admin3.sys.model.OcrEngineConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Component
public class InternalOcrProvider implements OcrProvider {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

  @Override
  public boolean supports(String providerType) {
    return "INTERNAL".equalsIgnoreCase(providerType);
  }

  @Override
  public String recognize(OcrEngineConfig config, OcrRecognitionRequest request) throws Exception {
    byte[] imageBytes = Base64.getDecoder().decode(request.imageBase64());
    String boundary = "----" + java.util.UUID.randomUUID();
    String lineSeparator = "\r\n";
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    baos.write(("--" + boundary + lineSeparator).getBytes());
    baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + request.filename() + "\"" + lineSeparator).getBytes());
    baos.write(("Content-Type: application/octet-stream" + lineSeparator).getBytes());
    baos.write(lineSeparator.getBytes());
    baos.write(imageBytes);
    baos.write(lineSeparator.getBytes());
    baos.write(("--" + boundary + "--" + lineSeparator).getBytes());

    String baseUrl = config.getBaseUrl() == null || config.getBaseUrl().isBlank()
      ? "http://127.0.0.1:8769"
      : config.getBaseUrl();
    HttpRequest httpRequest = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/ocr"))
      .timeout(REQUEST_TIMEOUT)
      .header("Content-Type", "multipart/form-data; boundary=" + boundary)
      .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
      .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("Internal OCR returned status " + response.statusCode() + ", body: " + response.body());
    }
    return response.body();
  }
}
