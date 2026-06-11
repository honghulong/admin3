package tech.wetech.admin3.infra.mcp.xiaoyi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.sys.model.User;
import tech.wetech.admin3.sys.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class XiaoYiAuthService {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthService.class);

  private static final long SESSION_EXPIRE_DAYS = 30;

  private final XiaoYiUserSessionRepository sessionRepository;
  private final UserRepository userRepository;

  public XiaoYiAuthService(XiaoYiUserSessionRepository sessionRepository, UserRepository userRepository) {
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public AuthorizeResult authorize(String authCode) {
    log.info("XiaoYi authorize request with authCode: {}", authCode);

    String phoneNumber = extractPhoneFromAuthCode(authCode);
    if (phoneNumber == null) {
      log.warn("Failed to extract phone number from authCode: {}", authCode);
      return AuthorizeResult.failure("无法从授权码中获取手机号");
    }

    User user = findUserByPhone(phoneNumber);
    if (user == null) {
      log.warn("No internal user found for phone number: {}", phoneNumber);
      return AuthorizeResult.failure("未找到对应的内部用户，请联系管理员绑定账号");
    }

    String agentLoginSessionId = UUID.randomUUID().toString().replace("-", "");
    LocalDateTime expireTime = LocalDateTime.now().plusDays(SESSION_EXPIRE_DAYS);

    XiaoYiUserSession session = new XiaoYiUserSession(agentLoginSessionId, phoneNumber, null, user, expireTime);
    sessionRepository.save(session);

    log.info("XiaoYi authorize success: agentLoginSessionId={}, phoneNumber={}, username={}",
      agentLoginSessionId, phoneNumber, user.getUsername());

    return AuthorizeResult.success(agentLoginSessionId);
  }

  @Transactional
  public DeauthorizeResult deauthorize(String agentLoginSessionId) {
    log.info("XiaoYi deauthorize request for agentLoginSessionId: {}", agentLoginSessionId);

    Optional<XiaoYiUserSession> sessionOpt = sessionRepository.findByAgentLoginSessionId(agentLoginSessionId);
    if (sessionOpt.isEmpty()) {
      log.warn("XiaoYi deauthorize failed: session not found for agentLoginSessionId: {}", agentLoginSessionId);
      return DeauthorizeResult.failure("会话不存在");
    }

    sessionRepository.delete(sessionOpt.get());
    log.info("XiaoYi deauthorize success: agentLoginSessionId={}", agentLoginSessionId);
    return DeauthorizeResult.success();
  }

  public Optional<XiaoYiUserSession> getSession(String agentLoginSessionId) {
    log.info("XiaoYi getSession: agentLoginSessionId={}", agentLoginSessionId);
    Optional<XiaoYiUserSession> session = sessionRepository.findByAgentLoginSessionId(agentLoginSessionId);
    if (session.isEmpty()) {
      log.warn("XiaoYi getSession: session not found for agentLoginSessionId={}", agentLoginSessionId);
      return Optional.empty();
    }
    if (session.get().isExpired()) {
      log.warn("XiaoYi getSession: session expired for agentLoginSessionId={}", agentLoginSessionId);
      return Optional.empty();
    }
    log.info("XiaoYi getSession: session found, username={}", session.get().getUser().getUsername());
    return session;
  }

  /**
   * ?? agentLoginSessionId ??????
   * ??????????? MCP ???????????
   */
  public CheckBindingResult checkBindingStatus(String agentLoginSessionId) {
    if (agentLoginSessionId == null || agentLoginSessionId.isBlank()) {
      log.warn("XiaoYi checkBindingStatus: agentLoginSessionId is null or empty");
      return CheckBindingResult.invalidSession("agentLoginSessionId 为空");
    }

    Optional<XiaoYiUserSession> sessionOpt = sessionRepository.findByAgentLoginSessionId(agentLoginSessionId);
    if (sessionOpt.isEmpty()) {
      log.warn("XiaoYi checkBindingStatus: session not found for agentLoginSessionId={}", agentLoginSessionId);
      return CheckBindingResult.invalidSession("会话不存在，请重新授权");
    }

    XiaoYiUserSession session = sessionOpt.get();
    if (session.isExpired()) {
      log.warn("XiaoYi checkBindingStatus: session expired for agentLoginSessionId={}", agentLoginSessionId);
      return CheckBindingResult.invalidSession("会话已过期，请重新授权");
    }

    User user = session.getUser();
    if (user == null) {
      log.warn("XiaoYi checkBindingStatus: session found but no user bound, agentLoginSessionId={}", agentLoginSessionId);
      return CheckBindingResult.unbound("用户尚未绑定员工身份，请先使用 bind_employee 工具完成绑定");
    }

    log.info("XiaoYi checkBindingStatus: bound, agentLoginSessionId={}, username={}, xEmployeeId={}",
      agentLoginSessionId, user.getUsername(), user.getXEmployeeId());
    return CheckBindingResult.bound(user.getUsername(), user.getXEmployeeId());
  }

  private String extractPhoneFromAuthCode(String authCode) {
    if (authCode == null || authCode.isBlank()) {
      return null;
    }
    return authCode.trim();
  }

  private User findUserByPhone(String phoneNumber) {
    return userRepository.findByUsername(phoneNumber).orElse(null);
  }

  public record AuthorizeResult(boolean success, String agentLoginSessionId, String errorMessage) {
    public static AuthorizeResult success(String agentLoginSessionId) {
      return new AuthorizeResult(true, agentLoginSessionId, null);
    }

    public static AuthorizeResult failure(String errorMessage) {
      return new AuthorizeResult(false, null, errorMessage);
    }
  }

  public record DeauthorizeResult(boolean ok, String errorMessage) {
    public static DeauthorizeResult success() {
      return new DeauthorizeResult(true, null);
    }

    public static DeauthorizeResult failure(String errorMessage) {
      return new DeauthorizeResult(false, errorMessage);
    }
  }

  /**
   * 绑定状态检查结果
   */
  public record CheckBindingResult(String status, String username, String xEmployeeId, String message) {
    /** 会话有效且已绑定内部员工 */
    public static CheckBindingResult bound(String username, String xEmployeeId) {
      return new CheckBindingResult("bound", username, xEmployeeId, null);
    }

    /** 会话有效但尚未绑定内部员工 */
    public static CheckBindingResult unbound(String message) {
      return new CheckBindingResult("unbound", null, null, message);
    }

    /** 会话无效（不存在或已过期） */
    public static CheckBindingResult invalidSession(String message) {
      return new CheckBindingResult("invalid", null, null, message);
    }

    public boolean isBound() {
      return "bound".equals(status);
    }
  }
}
