package tech.wetech.admin3.infra.mcp.xiaoyi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.wetech.admin3.sys.model.User;
import tech.wetech.admin3.sys.repository.UserRepository;

import java.util.Optional;

@Service
public class XiaoYiAuthService {

  private static final Logger log = LoggerFactory.getLogger(XiaoYiAuthService.class);

  private final UserRepository userRepository;

  public XiaoYiAuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Optional<User> findUserByEmployeeId(String xEmployeeId) {
    log.info("Looking up user by X-Employee-ID: {}", xEmployeeId);
    return userRepository.findByXEmployeeId(xEmployeeId);
  }
}
