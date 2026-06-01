package tech.wetech.admin3.infra.mcp.xiaoyi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/xiaoyi-mcp")
public class XiaoYiAuthController {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthController.class);

  private final XiaoYiAuthService authService;
  private final ObjectMapper objectMapper;

  public XiaoYiAuthController(XiaoYiAuthService authService, ObjectMapper objectMapper) {
    this.authService = authService;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public String handleAuth(@RequestBody String requestBody) {
    try {
      JsonNode requestJson = objectMapper.readTree(requestBody);
      String method = requestJson.has("method") ? requestJson.get("method").asText() : "";
      String id = requestJson.has("id") ? requestJson.get("id").asText() : "1";

      log.info("XiaoYi auth request received: method={}, id={}", method, id);

      return switch (method) {
        case "authorize" -> handleAuthorize(requestJson, id);
        case "deauthorize" -> handleDeauthorize(requestJson, id);
        default -> {
          log.warn("Unknown auth method: {}", method);
          yield buildErrorResponse(id, -32601, "未知方法: " + method);
        }
      };
    } catch (Exception e) {
      log.error("Failed to handle auth request", e);
      return buildErrorResponse("1", -32700, "解析请求失败: " + e.getMessage());
    }
  }

  private String handleAuthorize(JsonNode requestJson, String id) throws Exception {
    String authCode = extractAuthCode(requestJson);
    if (authCode == null) {
      return buildErrorResponse(id, -32602, "缺少 authCode");
    }

    XiaoYiAuthService.AuthorizeResult result = authService.authorize(authCode);

    if (result.success()) {
      ObjectNode response = objectMapper.createObjectNode();
      response.put("jsonrpc", "2.0");
      response.put("id", id);

      ObjectNode resultNode = response.putObject("result");
      resultNode.put("version", "1.0");
      resultNode.put("agentLoginSessionId", result.agentLoginSessionId());

      ObjectNode errorNode = response.putObject("error");
      errorNode.put("code", 0);
      errorNode.put("message", "success");

      log.info("XiaoYi authorize response: agentLoginSessionId={}", result.agentLoginSessionId());
      return objectMapper.writeValueAsString(response);
    } else {
      return buildErrorResponse(id, -32000, result.errorMessage());
    }
  }

  private String handleDeauthorize(JsonNode requestJson, String id) throws Exception {
    String agentLoginSessionId = extractAgentLoginSessionId(requestJson);
    if (agentLoginSessionId == null) {
      return buildErrorResponse(id, -32602, "缺少 agentLoginSessionId");
    }

    XiaoYiAuthService.DeauthorizeResult result = authService.deauthorize(agentLoginSessionId);

    if (result.success()) {
      ObjectNode response = objectMapper.createObjectNode();
      response.put("jsonrpc", "2.0");
      response.put("id", id);

      ObjectNode resultNode = response.putObject("result");
      resultNode.put("version", "1.0");

      ObjectNode errorNode = response.putObject("error");
      errorNode.put("code", 0);
      errorNode.put("message", "success");

      log.info("XiaoYi deauthorize success: agentLoginSessionId={}", agentLoginSessionId);
      return objectMapper.writeValueAsString(response);
    } else {
      return buildErrorResponse(id, -32000, result.errorMessage());
    }
  }

  private String extractAuthCode(JsonNode requestJson) {
    try {
      return requestJson.path("params").path("message").path("parts").get(0).path("data").path("authCode").asText(null);
    } catch (Exception e) {
      return null;
    }
  }

  private String extractAgentLoginSessionId(JsonNode requestJson) {
    try {
      return requestJson.path("params").path("message").path("parts").get(0).path("data").path("agentLoginSessionId").asText(null);
    } catch (Exception e) {
      return null;
    }
  }

  private String buildErrorResponse(String id, int code, String message) {
    try {
      ObjectNode response = objectMapper.createObjectNode();
      response.put("jsonrpc", "2.0");
      response.put("id", id);
      response.putObject("result");

      ObjectNode errorNode = response.putObject("error");
      errorNode.put("code", code);
      errorNode.put("message", message);
      return objectMapper.writeValueAsString(response);
    } catch (Exception e) {
      return "{\"jsonrpc\":\"2.0\",\"id\":\"" + id + "\",\"result\":{},\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\"}}";
    }
  }
}
