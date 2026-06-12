package tech.wetech.admin3.infra.mcp.xiaoyi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class XiaoYiBindingMcpConfig {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiBindingMcpConfig.class);

  @Bean
  McpJsonMapper xiaoyiBindingMcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  @Bean
  WebMvcStatelessServerTransport xiaoyiBindingWebMvcStatelessServerTransport(
    @Qualifier("xiaoyiBindingMcpJsonMapper") McpJsonMapper jsonMapper) {
    return WebMvcStatelessServerTransport.builder()
      .jsonMapper(jsonMapper)
      .messageEndpoint("/xiaoyi-mcp/binding/message")
      .securityValidator(ServerTransportSecurityValidator.NOOP)
      .contextExtractor(serverRequest -> {
        // 从 ServletRequest 属性中提取 agentLoginSessionId（由 XiaoYiAuthFilter 设置）
        HttpServletRequest servletRequest = (HttpServletRequest) serverRequest.servletRequest();
        String agentLoginSessionId = (String) servletRequest.getAttribute("XIAOYI_AGENT_LOGIN_SESSION_ID");
        if (agentLoginSessionId != null) {
          return McpTransportContext.create(java.util.Map.of("XIAOYI_AGENT_LOGIN_SESSION_ID", agentLoginSessionId));
        }
        return McpTransportContext.EMPTY;
      })
      .build();
  }

  @Bean
  RouterFunction<ServerResponse> xiaoyiBindingMcpRouterFunction(
    @Qualifier("xiaoyiBindingWebMvcStatelessServerTransport") WebMvcStatelessServerTransport transport) {
    return transport.getRouterFunction();
  }

  @Bean
  McpStatelessSyncServer xiaoyiBindingMcpStatelessSyncServer(
    @Qualifier("xiaoyiBindingWebMvcStatelessServerTransport") WebMvcStatelessServerTransport transport,
    XiaoYiAuthService authService,
    XiaoYiBindingService bindingService,
    @Qualifier("xiaoyiBindingMcpJsonMapper") McpJsonMapper jsonMapper) {
    return McpServer.sync(transport)
      .serverInfo("xiaoyi-binding-mcp-server", "1.0.0")
      .capabilities(McpSchema.ServerCapabilities.builder()
        .tools(true)
        .logging()
        .build())
      .tools(
        createCheckBindingStatusTool(authService, jsonMapper),
        createBindEmployeeTool(bindingService, jsonMapper))
      .build();
  }

  private McpStatelessServerFeatures.SyncToolSpecification createCheckBindingStatusTool(
    XiaoYiAuthService authService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("check_binding_status")
      .description("检查当前小艺会话的身份绑定状态。无需传入参数，agentLoginSessionId 由 XiaoYiAuthFilter 从 HTTP Header 自动提取。返回 bound(已绑定)/unbound(未绑定)/invalid(无效)")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {}
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // 从 McpTransportContext 中获取 agentLoginSessionId（由 contextExtractor 从请求属性中提取）
      String sessionId = (String) ctx.get("XIAOYI_AGENT_LOGIN_SESSION_ID");
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

  private McpStatelessServerFeatures.SyncToolSpecification createBindEmployeeTool(
    XiaoYiBindingService bindingService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("bind_employee")
      .description("绑定员工身份。将当前小艺会话与指定的员工ID绑定。需要传入 xEmployeeId（员工ID），agentLoginSessionId 由 XiaoYiAuthFilter 从 HTTP Header 自动提取。返回绑定结果")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "xEmployeeId": {
              "type": "string",
              "description": "员工ID（必填），例如 'EMP001'"
            }
          },
          "required": ["xEmployeeId"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // 从 McpTransportContext 中获取 agentLoginSessionId（由 contextExtractor 从请求属性中提取）
      String sessionId = (String) ctx.get("XIAOYI_AGENT_LOGIN_SESSION_ID");
      String xEmployeeId = request.arguments() != null ? String.valueOf(request.arguments().get("xEmployeeId")) : null;
      log.info("XiaoYi MCP tool called: bind_employee, agentLoginSessionId={}, xEmployeeId={}", sessionId, xEmployeeId);

      XiaoYiBindingService.BindEmployeeResult result = bindingService.bindEmployee(sessionId, xEmployeeId);

      String responseText;
      if (result.success()) {
        responseText = String.format("绑定成功\n用户名: %s\n员工ID: %s\n提示: %s",
          result.username(), result.xEmployeeId(), result.message());
        log.info("bind_employee success: username={}, xEmployeeId={}", result.username(), result.xEmployeeId());
      } else {
        responseText = "绑定失败\n提示: " + result.message();
        log.warn("bind_employee failed: {}", result.message());
      }

      return new McpSchema.CallToolResult(responseText, !result.success());
    });
  }
}
