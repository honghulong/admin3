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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Order(1)
@Configuration
@EnableConfigurationProperties
public class XiaoYiAuthFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthFilter.class);

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

    filterChain.doFilter(request, response);
  }
}
