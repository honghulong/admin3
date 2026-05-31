package tech.wetech.admin3.sys.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.sys.model.Leave;
import tech.wetech.admin3.sys.model.User;
import tech.wetech.admin3.sys.repository.LeaveRepository;
import tech.wetech.admin3.sys.repository.UserRepository;
import tech.wetech.admin3.sys.service.dto.LeaveDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@Service
public class LeaveService {

  private final LeaveRepository leaveRepository;
  private final UserRepository userRepository;

  public LeaveService(LeaveRepository leaveRepository, UserRepository userRepository) {
    this.leaveRepository = leaveRepository;
    this.userRepository = userRepository;
  }

  public PageDTO<LeaveDTO> findLeaves(Pageable pageable) {
    Page<Leave> page = leaveRepository.findAllByOrderByStartTimeDesc(pageable);
    return toLeaveDTOPage(page);
  }

  public PageDTO<LeaveDTO> findMyLeaves(Pageable pageable) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    User user = new User();
    user.setId(userInfo.userId());
    Page<Leave> page = leaveRepository.findByUserOrderByStartTimeDesc(user, pageable);
    return toLeaveDTOPage(page);
  }

  public LeaveDTO findLeaveById(Long leaveId) {
    Leave leave = leaveRepository.findById(leaveId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    return toLeaveDTO(leave);
  }

  public List<LeaveDTO> findLeavesByUsername(String username) {
    List<Leave> leaves = leaveRepository.findByUsername(username);
    return leaves.stream()
      .map(this::toLeaveDTO)
      .collect(Collectors.toList());
  }

  @Transactional
  public LeaveDTO createLeave(String leaveType, LocalDateTime startTime, LocalDateTime endTime, String leaveReason) {
    UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
    User user = new User();
    user.setId(userInfo.userId());
    return createLeaveInternal(user, leaveType, startTime, endTime, leaveReason);
  }

  @Transactional
  public LeaveDTO createLeaveByUsername(String username, String leaveType, LocalDateTime startTime, LocalDateTime endTime, String leaveReason) {
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST, "用户不存在: " + username));
    return createLeaveInternal(user, leaveType, startTime, endTime, leaveReason);
  }

  private LeaveDTO createLeaveInternal(User user, String leaveType, LocalDateTime startTime, LocalDateTime endTime, String leaveReason) {
    Leave leave = new Leave();
    leave.setUser(user);
    leave.setLeaveType(leaveType);
    leave.setStartTime(startTime);
    leave.setEndTime(endTime);
    leave.setLeaveReason(leaveReason);
    leave.setLeaveStatus("0");
    leave = leaveRepository.save(leave);
    return toLeaveDTO(leave);
  }

  @Transactional
  public LeaveDTO updateLeave(Long leaveId, String leaveType, LocalDateTime startTime, LocalDateTime endTime, String leaveReason) {
    Leave leave = leaveRepository.findById(leaveId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    leave.setLeaveType(leaveType);
    leave.setStartTime(startTime);
    leave.setEndTime(endTime);
    leave.setLeaveReason(leaveReason);
    leave = leaveRepository.save(leave);
    return toLeaveDTO(leave);
  }

  @Transactional
  public LeaveDTO approveLeave(Long leaveId) {
    Leave leave = leaveRepository.findById(leaveId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    leave.setLeaveStatus("1");
    leave = leaveRepository.save(leave);
    return toLeaveDTO(leave);
  }

  @Transactional
  public LeaveDTO rejectLeave(Long leaveId) {
    Leave leave = leaveRepository.findById(leaveId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    leave.setLeaveStatus("2");
    leave = leaveRepository.save(leave);
    return toLeaveDTO(leave);
  }

  @Transactional
  public LeaveDTO cancelLeave(Long leaveId) {
    Leave leave = leaveRepository.findById(leaveId)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    leave.setLeaveStatus("3");
    leave.setCancelTime(LocalDateTime.now());
    leave = leaveRepository.save(leave);
    return toLeaveDTO(leave);
  }

  @Transactional
  public void deleteLeave(Long leaveId) {
    leaveRepository.deleteById(leaveId);
  }

  private PageDTO<LeaveDTO> toLeaveDTOPage(Page<Leave> page) {
    List<LeaveDTO> list = page.getContent().stream()
      .map(this::toLeaveDTO)
      .collect(Collectors.toList());
    return new PageDTO<>(list, page.getTotalElements());
  }

  private LeaveDTO toLeaveDTO(Leave leave) {
    String statusLabel = switch (leave.getLeaveStatus()) {
      case "0" -> "刚提交";
      case "1" -> "审核通过";
      case "2" -> "被退回";
      case "3" -> "已销假";
      default -> leave.getLeaveStatus();
    };
    String typeLabel = switch (leave.getLeaveType()) {
      case "sick" -> "病假";
      case "personal" -> "事假";
      case "maternity" -> "产假";
      case "offshift" -> "调休";
      default -> leave.getLeaveType();
    };
    return new LeaveDTO(
      leave.getId(),
      leave.getUser(),
      leave.getLeaveType(),
      typeLabel,
      leave.getStartTime(),
      leave.getEndTime(),
      leave.getLeaveReason(),
      leave.getLeaveStatus(),
      statusLabel,
      leave.getCancelTime()
    );
  }
}
