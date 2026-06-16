package tech.wetech.admin3.infra.mcp.xiaoyi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import tech.wetech.admin3.sys.model.BaseEntity;
import tech.wetech.admin3.sys.model.User;

import java.time.LocalDateTime;

@Entity
public class XiaoYiUserSession extends BaseEntity {

  // 小艺设备标识（deviceInfo.sid），设备唯一标识，有则优先使用
  @Column(unique = true)
  private String sid;

  // 小艺会话ID（session.sessionId），每次会话不同
  @Column
  private String sessionId;

  // 保留原 agentLoginSessionId 字段（华为授权后才有，目前未使用）
  @Column
  private String agentLoginSessionId;

  @Column
  private String phoneNumber;

  @Column
  private String huaweiOpenId;

  @ManyToOne
  private User user;

  @Column(nullable = false)
  private LocalDateTime createdTime;

  @Column(nullable = false)
  private LocalDateTime expireTime;

  public XiaoYiUserSession() {
  }

  public XiaoYiUserSession(String agentLoginSessionId, String phoneNumber, String huaweiOpenId, User user, LocalDateTime expireTime) {
    this.agentLoginSessionId = agentLoginSessionId;
    this.phoneNumber = phoneNumber;
    this.huaweiOpenId = huaweiOpenId;
    this.user = user;
    this.createdTime = LocalDateTime.now();
    this.expireTime = expireTime;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getAgentLoginSessionId() {
    return agentLoginSessionId;
  }

  public void setAgentLoginSessionId(String agentLoginSessionId) {
    this.agentLoginSessionId = agentLoginSessionId;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getHuaweiOpenId() {
    return huaweiOpenId;
  }

  public void setHuaweiOpenId(String huaweiOpenId) {
    this.huaweiOpenId = huaweiOpenId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public LocalDateTime getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(LocalDateTime createdTime) {
    this.createdTime = createdTime;
  }

  public LocalDateTime getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(LocalDateTime expireTime) {
    this.expireTime = expireTime;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expireTime);
  }
}
