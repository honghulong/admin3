package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IncomingDocumentOcrMapper {

  private static final Set<String> SYSTEM_KEYS = Set.of(
    "code", "status", "success", "message", "msg", "error", "errors", "request_id", "requestId", "trace_id", "traceId"
  );

  private final ObjectMapper objectMapper;

  public IncomingDocumentOcrMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, String> extractBusinessFields(String rawJson) {
    Map<String, String> fields = new LinkedHashMap<>();
    if (rawJson == null || rawJson.isBlank()) return fields;
    try {
      JsonNode root = objectMapper.readTree(rawJson);
      if (root.path("pages").isArray()) {
        extractPageLines(root.path("pages"), fields);
        if (!fields.isEmpty()) return fields;
      }
      JsonNode businessRoot = root.has("data") ? root.get("data") : root;
      flatten("", businessRoot, fields);
      return fields;
    } catch (Exception e) {
      fields.put("ocr_text", rawJson);
      return fields;
    }
  }

  private void flatten(String prefix, JsonNode node, Map<String, String> fields) {
    if (node == null || node.isNull()) return;
    if (node.isValueNode()) {
      if (!prefix.isBlank()) fields.put(prefix, node.asText());
      return;
    }
    if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        flatten(prefix.isBlank() ? String.valueOf(i) : prefix + "." + i, node.get(i), fields);
      }
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonNode> entry = iterator.next();
      String key = entry.getKey();
      if (SYSTEM_KEYS.contains(key)) continue;
      JsonNode child = entry.getValue();
      if (prefix.isBlank() && isWrapperKey(key) && !child.isValueNode()) {
        flatten("", child, fields);
      } else {
        flatten(prefix.isBlank() ? key : prefix + "." + key, child, fields);
      }
    }
  }

  private void extractPageLines(JsonNode pages, Map<String, String> fields) {
    int index = 1;
    StringBuilder plainText = new StringBuilder();
    for (JsonNode page : pages) {
      JsonNode lines = page.path("lines");
      if (!lines.isArray()) continue;
      int pageNo = page.path("page_no").asInt(1);
      for (JsonNode line : lines) {
        String text = line.path("text").asText("");
        if (text.isBlank()) continue;
        String key = String.format("line.%03d", index++);
        fields.put(key, formatLineValue(pageNo, line, text));
        if (!plainText.isEmpty()) plainText.append('\n');
        plainText.append(text);
      }
    }
    if (!plainText.isEmpty()) {
      fields.put("plain_text", plainText.toString());
    }
  }

  private String formatLineValue(int pageNo, JsonNode line, String text) {
    double confidence = line.path("confidence").asDouble(0);
    int x1 = (int) Math.round(line.path("x1").asDouble(0));
    int y1 = (int) Math.round(line.path("y1").asDouble(0));
    int x2 = (int) Math.round(line.path("x2").asDouble(0));
    int y2 = (int) Math.round(line.path("y2").asDouble(0));
    return text + " | p" + pageNo + " | conf " + String.format("%.3f", confidence) + " | box " + x1 + "," + y1 + "," + x2 + "," + y2;
  }

  private boolean isWrapperKey(String key) {
    return "result".equals(key) || "response".equals(key) || "content".equals(key);
  }
}
