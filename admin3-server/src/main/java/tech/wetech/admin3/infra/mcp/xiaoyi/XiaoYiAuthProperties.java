package tech.wetech.admin3.infra.mcp.xiaoyi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "xiaoyi.mcp.auth")
public class XiaoYiAuthProperties {

  private String appId = "admin3-leave-app";
  private String apiKey = "admin3-leave-api-key-2026";

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
