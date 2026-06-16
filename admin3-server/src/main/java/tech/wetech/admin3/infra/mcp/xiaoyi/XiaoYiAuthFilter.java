package tech.wetech.admin3.infra.mcp.xiaoyi;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Enumeration;
import java.util.TreeMap;

@Order(1)
@Configuration
@EnableConfigurationProperties
public class XiaoYiAuthFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthFilter.class);

  /** 请求属性名，存储从 x-request-id 提取的 sessionId */
  public static final String ATTR_SESSION_ID = "XIAOYI_SESSION_ID_FROM_X_REQUEST_ID";

  private final XiaoYiAuthProperties authProperties;

  public XiaoYiAuthFilter(XiaoYiAuthProperties authProperties) {
    this.authProperties = authProperties;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String path = request.getRequestURI();
    if (!path.startsWith("/admin3/xiaoyi-mcp/")) {
      filterChain.doFilter(request, response);
      return;
    }

    log.info("XiaoYi MCP request: {} {}", request.getMethod(), path);

    // 记录所有请求头
    Enumeration<String> headerNames = request.getHeaderNames();
    TreeMap<String, String> sortedHeaders = new TreeMap<>();
    while (headerNames != null && headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      sortedHeaders.put(name, request.getHeader(name));
    }
    StringBuilder sb = new StringBuilder("XiaoYi MCP full headers: {");
    sortedHeaders.forEach((k, v) -> sb.append(k).append('=').append(v).append(", "));
    if (!sortedHeaders.isEmpty()) sb.setLength(sb.length() - 2);
    sb.append('}');
    log.info(sb.toString());

    // 从 x-request-id 提取 sessionId（格式：sessionId&1-random-uuid）
    String xRequestId = request.getHeader("x-request-id");
    if (xRequestId != null && !xRequestId.isBlank()) {
      int ampIndex = xRequestId.indexOf('&');
      String sessionIdFromHeader = (ampIndex > 0) ? xRequestId.substring(0, ampIndex) : xRequestId;
      if (!sessionIdFromHeader.isBlank()) {
        request.setAttribute(ATTR_SESSION_ID, sessionIdFromHeader);
        log.info("XiaoYi extracted sessionId from x-request-id: {}", sessionIdFromHeader);
      }
    }

    // 重点关注小艺平台透传的用户上下文 header
    String xUserId = request.getHeader("x-user-id");
    String xDeviceId = request.getHeader("x-device-id");
    String xSessionId = request.getHeader("x-session-id");
    String xTimestamp = request.getHeader("x-timestamp");
    String xSignature = request.getHeader("x-signature");
    if (xUserId != null || xDeviceId != null || xSessionId != null) {
      log.info("XiaoYi context - x-user-id={}, x-device-id={}, x-session-id={}, x-timestamp={}, x-signature={}",
        xUserId, xDeviceId, xSessionId, xTimestamp, xSignature);
    }

    String appId = request.getHeader("appId");
    String apiKey = request.getHeader("apiKey");

    if (appId == null || apiKey == null) {
      log.warn("XiaoYi MCP request missing appId or apiKey header: {} {}", request.getMethod(), path);
      response.setStatus(401);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Missing authentication headers: appId and apiKey are required\"}");
      return;
    }

    if (!authProperties.getAppId().equals(appId) || !authProperties.getApiKey().equals(apiKey)) {
      log.warn("XiaoYi MCP request invalid appId or apiKey: {} {}", request.getMethod(), path);
      response.setStatus(401);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Invalid appId or apiKey\"}");
      return;
    }

    log.info("XiaoYi MCP auth success: {} {}, appId={}", request.getMethod(), path, appId);

    filterChain.doFilter(request, response);
  }
}
