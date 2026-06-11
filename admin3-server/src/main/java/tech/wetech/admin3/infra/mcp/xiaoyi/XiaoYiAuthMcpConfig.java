package tech.wetech.admin3.infra.mcp.xiaoyi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import tech.wetech.admin3.common.SessionItemHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class XiaoYiAuthMcpConfig {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthMcpConfig.class);

  @Bean
  McpJsonMapper xiaoyiAuthMcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  @Bean
  WebMvcStatelessServerTransport xiaoyiAuthWebMvcStatelessServerTransport(
    @Qualifier("xiaoyiAuthMcpJsonMapper") McpJsonMapper jsonMapper) {
    return WebMvcStatelessServerTransport.builder()
      .jsonMapper(jsonMapper)
      .messageEndpoint("/xiaoyi-mcp/auth/message")
      .securityValidator(ServerTransportSecurityValidator.NOOP)
      .build();
  }

  @Bean
  RouterFunction<ServerResponse> xiaoyiAuthMcpRouterFunction(
    @Qualifier("xiaoyiAuthWebMvcStatelessServerTransport") WebMvcStatelessServerTransport transport) {
    return transport.getRouterFunction();
  }

  @Bean
  McpStatelessSyncServer xiaoyiAuthMcpStatelessSyncServer(
    @Qualifier("xiaoyiAuthWebMvcStatelessServerTransport") WebMvcStatelessServerTransport transport,
    XiaoYiAuthService authService,
    @Qualifier("xiaoyiAuthMcpJsonMapper") McpJsonMapper jsonMapper) {
    return McpServer.sync(transport)
      .serverInfo("xiaoyi-auth-mcp-server", "1.0.0")
      .capabilities(McpSchema.ServerCapabilities.builder()
        .tools(true)
        .logging()
        .build())
      .tools(createCheckBindingStatusTool(authService, jsonMapper))
      .build();
  }

  private McpStatelessServerFeatures.SyncToolSpecification createCheckBindingStatusTool(
    XiaoYiAuthService authService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("check_binding_status")
      .description("检查当前小艺会话的身份绑定状态。可选参数 agentLoginSessionId，不传则自动从请求头读取。返回 bound(已绑定)/unbound(未绑定)/invalid(无效)")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "agentLoginSessionId": {
              "type": "string",
              "description": "会话ID（可选，不传则自动从请求头读取）"
            }
          }
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // 优先从工具参数读取，其次从请求头(ThreadLocal)读取
      String sessionId = null;
      if (request.arguments() != null && request.arguments().containsKey("agentLoginSessionId")) {
        sessionId = String.valueOf(request.arguments().get("agentLoginSessionId"));
      }
      if (sessionId == null || sessionId.isBlank() || "null".equals(sessionId)) {
        sessionId = (String) SessionItemHolder.getItem("XIAOYI_AGENT_LOGIN_SESSION_ID");
      }
      log.info("XiaoYi MCP tool called: check_binding_status, agentLoginSessionId={}", sessionId);

      XiaoYiAuthService.CheckBindingResult result = authService.checkBindingStatus(sessionId);

      String responseText;
      switch (result.status()) {
        case "bound":
          responseText = String.format("绑定状态: bound\n用户名: %s\n员工ID: %s",
            result.username(), result.xEmployeeId());
          log.info("check_binding_status: bound, username={}, xEmployeeId={}",
            result.username(), result.xEmployeeId());
          break;
        case "unbound":
          responseText = "绑定状态: unbound\n提示: " + result.message();
          log.warn("check_binding_status: unbound, agentLoginSessionId={}", sessionId);
          break;
        default:
          responseText = "绑定状态: invalid\n提示: " + result.message();
          log.warn("check_binding_status: invalid, agentLoginSessionId={}, reason={}",
            sessionId, result.message());
          break;
      }

      return new McpSchema.CallToolResult(responseText, !result.isBound());
    });
  }
}
