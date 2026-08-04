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

  private boolean isWrapperKey(String key) {
    return "result".equals(key) || "response".equals(key) || "content".equals(key);
  }
}
