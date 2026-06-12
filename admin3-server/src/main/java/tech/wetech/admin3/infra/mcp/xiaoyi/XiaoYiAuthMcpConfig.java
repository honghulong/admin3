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
      .tools(
        createAuthorizeTool(authService, jsonMapper),
        createDeauthorizeTool(authService, jsonMapper))
      .build();
  }

  private McpStatelessServerFeatures.SyncToolSpecification createAuthorizeTool(
    XiaoYiAuthService authService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("authorize")
      .description("用户授权登录。接收华为账号授权码 authCode，返回 agentLoginSessionId。小艺用户在智能体内点击账号授权后调用此工具。")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "authCode": {
              "type": "string",
              "description": "华为账号授权码（必填），由小艺客户端在用户授权后提供"
            }
          },
          "required": ["authCode"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      String authCode = request.arguments() != null ? String.valueOf(request.arguments().get("authCode")) : null;
      log.info("XiaoYi MCP tool called: authorize, authCode={}", authCode);

      XiaoYiAuthService.AuthorizeResult result = authService.authorize(authCode);

      String responseText;
      if (result.success()) {
        responseText = String.format("授权成功\nagentLoginSessionId: %s", result.agentLoginSessionId());
        log.info("authorize success: agentLoginSessionId={}", result.agentLoginSessionId());
      } else {
        responseText = "授权失败\n提示: " + result.errorMessage();
        log.warn("authorize failed: {}", result.errorMessage());
      }

      return new McpSchema.CallToolResult(responseText, !result.success());
    });
  }

  private McpStatelessServerFeatures.SyncToolSpecification createDeauthorizeTool(
    XiaoYiAuthService authService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("deauthorize")
      .description("用户解授权。清除指定 agentLoginSessionId 的会话。小艺用户在智能体内点击解绑账号时调用此工具。")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "agentLoginSessionId": {
              "type": "string",
              "description": "要清除的会话ID（必填）"
            }
          },
          "required": ["agentLoginSessionId"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      String agentLoginSessionId = request.arguments() != null ? String.valueOf(request.arguments().get("agentLoginSessionId")) : null;
      log.info("XiaoYi MCP tool called: deauthorize, agentLoginSessionId={}", agentLoginSessionId);

      XiaoYiAuthService.DeauthorizeResult result = authService.deauthorize(agentLoginSessionId);

      String responseText;
      if (result.ok()) {
        responseText = "解授权成功";
        log.info("deauthorize success: agentLoginSessionId={}", agentLoginSessionId);
      } else {
        responseText = "解授权失败\n提示: " + result.errorMessage();
        log.warn("deauthorize failed: {}", result.errorMessage());
      }

      return new McpSchema.CallToolResult(responseText, !result.ok());
    });
  }
}
