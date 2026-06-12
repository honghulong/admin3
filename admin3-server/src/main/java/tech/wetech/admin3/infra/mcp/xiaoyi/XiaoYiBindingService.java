package tech.wetech.admin3.infra.mcp.xiaoyi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.sys.model.User;
import tech.wetech.admin3.sys.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class XiaoYiBindingService {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiBindingService.class);

  private static final long SESSION_EXPIRE_DAYS = 30;

  private final XiaoYiUserSessionRepository sessionRepository;
  private final UserRepository userRepository;

  public XiaoYiBindingService(XiaoYiUserSessionRepository sessionRepository, UserRepository userRepository) {
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
  }

  /**
   * 绑定员工：将 agentLoginSessionId 与 xEmployeeId 对应的 User 关联
   */
  @Transactional
  public BindEmployeeResult bindEmployee(String agentLoginSessionId, String xEmployeeId) {
    log.info("XiaoYi bindEmployee: agentLoginSessionId={}, xEmployeeId={}", agentLoginSessionId, xEmployeeId);

    if (agentLoginSessionId == null || agentLoginSessionId.isBlank()) {
      return BindEmployeeResult.failure("agentLoginSessionId 为空");
    }
    if (xEmployeeId == null || xEmployeeId.isBlank()) {
      return BindEmployeeResult.failure("员工ID不能为空");
    }

    // 1. 核查员工ID是否存在
    Optional<User> userOpt = userRepository.findByXEmployeeId(xEmployeeId);
    if (userOpt.isEmpty()) {
      log.warn("XiaoYi bindEmployee: xEmployeeId not found: {}", xEmployeeId);
      return BindEmployeeResult.failure("员工ID不存在，请检查输入的员工ID是否正确");
    }

    // 2. 查找或创建 XiaoYiUserSession
    Optional<XiaoYiUserSession> sessionOpt = sessionRepository.findByAgentLoginSessionId(agentLoginSessionId);
    XiaoYiUserSession session;
    if (sessionOpt.isPresent()) {
      session = sessionOpt.get();
      if (session.isExpired()) {
        log.warn("XiaoYi bindEmployee: session expired, agentLoginSessionId={}", agentLoginSessionId);
        return BindEmployeeResult.failure("会话已过期，请重新授权");
      }
    } else {
      // 如果 session 不存在，创建一个新的（设置30天过期）
      session = new XiaoYiUserSession();
      session.setAgentLoginSessionId(agentLoginSessionId);
      session.setCreatedTime(LocalDateTime.now());
      session.setExpireTime(LocalDateTime.now().plusDays(SESSION_EXPIRE_DAYS));
    }

    // 3. 绑定员工
    User user = userOpt.get();
    session.setUser(user);
    sessionRepository.save(session);

    log.info("XiaoYi bindEmployee success: agentLoginSessionId={}, xEmployeeId={}, username={}",
      agentLoginSessionId, xEmployeeId, user.getUsername());
    return BindEmployeeResult.success(user.getUsername(), user.getXEmployeeId());
  }

  /**
   * 绑定员工结果
   */
  public record BindEmployeeResult(boolean success, String username, String xEmployeeId, String message) {
    public static BindEmployeeResult success(String username, String xEmployeeId) {
      return new BindEmployeeResult(true, username, xEmployeeId, "绑定成功");
    }

    public static BindEmployeeResult failure(String message) {
      return new BindEmployeeResult(false, null, null, message);
    }
  }
}
