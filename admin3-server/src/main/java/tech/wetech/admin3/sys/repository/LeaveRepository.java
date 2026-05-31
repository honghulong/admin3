package tech.wetech.admin3.sys.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.wetech.admin3.sys.model.Leave;
import tech.wetech.admin3.sys.model.User;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

  Page<Leave> findByUserOrderByStartTimeDesc(User user, Pageable pageable);

  Page<Leave> findAllByOrderByStartTimeDesc(Pageable pageable);

  @Query("select l from Leave l where l.user.username = :username order by l.startTime desc")
  List<Leave> findByUsername(String username);

}
