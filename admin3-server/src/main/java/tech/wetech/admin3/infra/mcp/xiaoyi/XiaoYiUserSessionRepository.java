package tech.wetech.admin3.infra.mcp.xiaoyi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XiaoYiUserSessionRepository extends JpaRepository<XiaoYiUserSession, Long> {

  Optional<XiaoYiUserSession> findByAgentLoginSessionId(String agentLoginSessionId);

  Optional<XiaoYiUserSession> findByPhoneNumber(String phoneNumber);

  Optional<XiaoYiUserSession> findByHuaweiOpenId(String huaweiOpenId);
}
