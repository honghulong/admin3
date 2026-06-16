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
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class XiaoYiBindingMcpConfig {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiBindingMcpConfig.class);

  /**
   * 同一对话内缓存 sessionId → sid 映射。
   * check_binding_status（含 deviceInfo.sid）在 bind_employee 之前调用，
   * bind_employee fallback 时通过 sessionId 取出 sid，两个字段都保存到数据库。
   */
  static final java.util.concurrent.ConcurrentHashMap<String, String> sidCache =
    new java.util.concurrent.ConcurrentHashMap<>();

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
        HttpServletRequest servletRequest = (HttpServletRequest) serverRequest.servletRequest();
        String sessionId = (String) servletRequest.getAttribute(XiaoYiAuthFilter.ATTR_SESSION_ID);
        if (sessionId != null) {
          return McpTransportContext.create(java.util.Map.of("XIAOYI_SESSION_ID_FROM_HEADER", sessionId));
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
      .description("检查当前小艺会话的身份绑定状态。无需传入参数，系统自动从请求中提取设备标识（sid/sessionId）。返回 bound(已绑定)/unbound(未绑定)/invalid(无效)")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {}
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      // 从 arguments 中提取设备标识（sid 或 sessionId）
      String sid = authService.resolveDeviceKey(request.arguments());
      // 从 x-request-id 取 sessionId（用于 bind_employee 等无 deviceInfo 的工具调用）
      String sessionIdFromHeader = (String) ctx.get("XIAOYI_SESSION_ID_FROM_HEADER");

      if (request.arguments() != null) {
        log.info("XiaoYi MCP tool called: check_binding_status, sid={}, args={}", sid, request.arguments());

        // 缓存 sessionId → sid 映射，供同一对话的 bind_employee 使用
        try {
          Object sessionObj = request.arguments().get("session");
          String argsSessionId = null;
          if (sessionObj instanceof java.util.Map) {
            argsSessionId = String.valueOf(((java.util.Map<?,?>) sessionObj).get("sessionId"));
            if ("null".equals(argsSessionId)) argsSessionId = null;
          }
          Object deviceInfoObj = request.arguments().get("deviceInfo");
          String argsSid = null;
          if (deviceInfoObj instanceof java.util.Map) {
            argsSid = String.valueOf(((java.util.Map<?,?>) deviceInfoObj).get("sid"));
            if ("null".equals(argsSid)) argsSid = null;
          }
          if (argsSessionId != null && argsSid != null) {
            sidCache.put(argsSessionId, argsSid);
            log.info("XiaoYi cached sid mapping: sessionId={}, sid={}", argsSessionId, argsSid);
          }
        } catch (Exception e) {
          log.warn("XiaoYi failed to cache sid mapping", e);
        }

        // 单独提取 userId（小艺平台的匿名用户ID，保留结构但不用于验证）
        Object userAuthObj = request.arguments().get("userAuth");
        if (userAuthObj != null) {
          log.info("XiaoYi userAuth in arguments: {}", userAuthObj);
          if (userAuthObj instanceof java.util.Map) {
            Object userObj = ((java.util.Map<?,?>) userAuthObj).get("user");
            if (userObj instanceof java.util.Map) {
              String userId = String.valueOf(((java.util.Map<?,?>) userObj).get("userId"));
              if (userId != null && !userId.isBlank() && !"null".equals(userId)) {
                log.info("XiaoYi userId from arguments.userAuth.user: {}", userId);
              }
            }
          }
        }
      } else {
        log.info("XiaoYi MCP tool called: check_binding_status, sid={}", sid);
      }

      // 两个条件有一个对上就认：先用 sid 查，再用 sessionId 查
      XiaoYiAuthService.CheckBindingResult result = authService.checkBindingStatus(sid, sessionIdFromHeader);

      String responseText;
      switch (result.status()) {
        case "bound":
          responseText = String.format("绑定状态: bound\n用户名: %s\n员工ID: %s",
            result.username(), result.xEmployeeId());
          log.info("check_binding_status: bound, sid={}, sessionId={}, username={}, xEmployeeId={}",
            sid, sessionIdFromHeader, result.username(), result.xEmployeeId());
          break;
        case "unbound":
          responseText = "绑定状态: unbound\n提示: " + result.message();
          log.warn("check_binding_status: unbound, sid={}, sessionId={}", sid, sessionIdFromHeader);
          break;
        default:
          responseText = "绑定状态: invalid\n提示: " + result.message();
          log.warn("check_binding_status: invalid, sid={}, sessionId={}, reason={}",
            sid, sessionIdFromHeader, result.message());
          break;
      }

      return new McpSchema.CallToolResult(responseText, !result.isBound());
    });
  }

  private McpStatelessServerFeatures.SyncToolSpecification createBindEmployeeTool(
    XiaoYiBindingService bindingService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("bind_employee")
      .description("绑定员工身份。将当前小艺会话与指定的员工ID绑定。需要传入 xEmployeeId（员工ID），系统自动从请求中提取设备标识（sid/sessionId）。返回绑定结果")
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
      // 从 arguments 中提取设备标识和员工ID
      String xEmployeeId = request.arguments() != null ? String.valueOf(request.arguments().get("xEmployeeId")) : null;

      // 提取 sid 和 sessionId
      String sid = null;
      String sessionId = null;
      if (request.arguments() != null) {
        try {
          Object deviceInfoObj = request.arguments().get("deviceInfo");
          if (deviceInfoObj instanceof java.util.Map) {
            sid = String.valueOf(((java.util.Map<?,?>) deviceInfoObj).get("sid"));
            if ("null".equals(sid) || sid == null) sid = null;
          }
          Object sessionObj = request.arguments().get("session");
          if (sessionObj instanceof java.util.Map) {
            sessionId = String.valueOf(((java.util.Map<?,?>) sessionObj).get("sessionId"));
            if ("null".equals(sessionId) || sessionId == null) sessionId = null;
          }
        } catch (Exception e) {
          log.warn("bind_employee: failed to extract device identifiers", e);
        }
      }
      // 如果 arguments 中取不到，回退到 x-request-id header 提取的 sessionId
      if ((sid == null || sid.isBlank()) && (sessionId == null || sessionId.isBlank())) {
        String fallback = (String) ctx.get("XIAOYI_SESSION_ID_FROM_HEADER");
        if (fallback != null) {
          sessionId = fallback;
          log.info("XiaoYi bind_employee fallback to sessionId from x-request-id: {}", sessionId);
          // 从缓存中取 sid（check_binding_status 已缓存）
          String cachedSid = sidCache.remove(fallback);
          if (cachedSid != null) {
            sid = cachedSid;
            log.info("XiaoYi bind_employee retrieved sid from cache: {}", sid);
          }
        }
      }
      log.info("XiaoYi MCP tool called: bind_employee, sid={}, sessionId={}, xEmployeeId={}", sid, sessionId, xEmployeeId);

      XiaoYiBindingService.BindEmployeeResult result = bindingService.bindEmployee(sid, sessionId, xEmployeeId);

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
