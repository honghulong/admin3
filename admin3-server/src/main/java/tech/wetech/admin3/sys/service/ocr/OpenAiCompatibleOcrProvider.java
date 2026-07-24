package tech.wetech.admin3.sys.service.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import tech.wetech.admin3.sys.model.OcrEngineConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenAiCompatibleOcrProvider implements OcrProvider {

  static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
  private static final String DEFAULT_PROMPT = """
    Extract structured fields from a Chinese invoice image and return one JSON object only.
    Use snake_case fields: invoice_type, invoice_no, invoice_code, date, buyer_name, buyer_tax_id,
    buyer_address_phone, buyer_bank_info, seller_name, seller_tax_id, seller_address_phone,
    seller_bank_info, total_amount, amount_before_tax, tax_amount, tax_rate, check_code,
    machine_no, payee, reviewer, issuer, remarks, start_place, end_place.
    Never skip buyer_address_phone, buyer_bank_info, seller_address_phone, seller_bank_info if visible.
    Use empty strings for fields that cannot be recognized. Do not return null or the string "null".
    """;

  private final ObjectMapper objectMapper;
  private final OcrSecretCipher secretCipher;

  public OpenAiCompatibleOcrProvider(ObjectMapper objectMapper, OcrSecretCipher secretCipher) {
    this.objectMapper = objectMapper;
    this.secretCipher = secretCipher;
  }

  @Override
  public boolean supports(String providerType) {
    return "QWEN".equalsIgnoreCase(providerType)
      || "KIMI".equalsIgnoreCase(providerType)
      || "OPENAI_COMPATIBLE".equalsIgnoreCase(providerType);
  }

  @Override
  public String recognize(OcrEngineConfig config, OcrRecognitionRequest request) throws Exception {
    String apiKey = secretCipher.decrypt(config.getApiKeyCipherText());
    if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("OCR engine API key is required");
    if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) throw new IllegalArgumentException("OCR engine baseUrl is required");
    if (config.getModelName() == null || config.getModelName().isBlank()) throw new IllegalArgumentException("OCR engine modelName is required");

    ObjectNode body = buildRequestBody(config, request);

    HttpRequest httpRequest = HttpRequest.newBuilder()
      .uri(URI.create(config.getBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
      .timeout(REQUEST_TIMEOUT)
      .header("Authorization", "Bearer " + apiKey)
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
      .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("OpenAI-compatible OCR returned status " + response.statusCode() + ", body: " + response.body());
    }
    return normalizeResponse(response.body(), config.getProviderType());
  }

  String normalizeResponse(String responseBody, String providerType) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    String content = root.at("/choices/0/message/content").asText("");
    ObjectNode normalized = objectMapper.createObjectNode();
    normalized.put("status", "ok");
    normalized.put("source_type", providerType);
    normalized.put("ocr_text", content);
    JsonNode fields = parseJsonContent(content);
    normalized.set("invoice_fields", fields == null ? objectMapper.createObjectNode() : normalizeFields(fields));
    return objectMapper.writeValueAsString(normalized);
  }

  ObjectNode buildRequestBody(OcrEngineConfig config, OcrRecognitionRequest request) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", config.getModelName());
    if ("KIMI".equalsIgnoreCase(config.getProviderType())) {
      body.putObject("thinking").put("type", "disabled");
    }
    ArrayNode messages = body.putArray("messages");
    ObjectNode user = messages.addObject();
    user.put("role", "user");
    ArrayNode content = user.putArray("content");
    ObjectNode image = content.addObject();
    image.put("type", "image_url");
    ObjectNode imageUrl = image.putObject("image_url");
    imageUrl.put("url", "data:" + mimeType(request.filename()) + ";base64," + request.imageBase64());
    ObjectNode text = content.addObject();
    text.put("type", "text");
    text.put("text", firstNonBlank(request.prompt(), config.getPrompt(), DEFAULT_PROMPT));
    return body;
  }

  private JsonNode parseJsonContent(String content) {
    if (content == null || content.isBlank()) return null;
    String trimmed = content.trim();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
    }
    try {
      JsonNode node = objectMapper.readTree(trimmed);
      return node.isObject() ? node : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  private ObjectNode normalizeFields(JsonNode fields) {
    ObjectNode normalized = objectMapper.createObjectNode();
    Iterator<Map.Entry<String, JsonNode>> iterator = fields.fields();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonNode> entry = iterator.next();
      String value = entry.getValue().isNull() ? "" : entry.getValue().asText("");
      normalized.put(entry.getKey(), isNullText(value) ? "" : value);
    }

    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put("invoice_number", "invoice_no");
    aliases.put("invoice_num", "invoice_no");
    aliases.put("invoice_date", "date");
    aliases.put("drawer", "issuer");
    aliases.put("total_tax_included_amount", "total_amount");
    aliases.put("amount", "total_amount");
    aliases.put("tax_excluded_amount", "amount_before_tax");
    aliases.put("buyer_address", "buyer_address_phone");
    aliases.put("buyer_phone", "buyer_address_phone");
    aliases.put("buyer_address_and_phone", "buyer_address_phone");
    aliases.put("buyer_address_tel", "buyer_address_phone");
    aliases.put("buyer_bank", "buyer_bank_info");
    aliases.put("buyer_account", "buyer_bank_info");
    aliases.put("buyer_bank_account", "buyer_bank_info");
    aliases.put("buyer_bank_and_account", "buyer_bank_info");
    aliases.put("seller_address", "seller_address_phone");
    aliases.put("seller_phone", "seller_address_phone");
    aliases.put("seller_address_and_phone", "seller_address_phone");
    aliases.put("seller_address_tel", "seller_address_phone");
    aliases.put("seller_bank", "seller_bank_info");
    aliases.put("seller_account", "seller_bank_info");
    aliases.put("seller_bank_account", "seller_bank_info");
    aliases.put("seller_bank_and_account", "seller_bank_info");
    aliases.forEach((from, to) -> {
      if ((!normalized.has(to) || normalized.path(to).asText("").isBlank()) && normalized.has(from)) {
        normalized.put(to, normalized.path(from).asText(""));
      }
    });
    return normalized;
  }

  private boolean isNullText(String value) {
    return value == null || value.trim().isBlank() || "null".equalsIgnoreCase(value.trim()) || "none".equalsIgnoreCase(value.trim());
  }

  private String mimeType(String filename) {
    String lower = filename == null ? "" : filename.toLowerCase();
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".webp")) return "image/webp";
    return "image/png";
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }
}
