package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.User;

import java.time.LocalDateTime;

public record LeaveDTO(
  Long id,
  User user,
  String leaveType,
  String leaveTypeLabel,
  LocalDateTime startTime,
  LocalDateTime endTime,
  String leaveReason,
  String leaveStatus,
  String leaveStatusLabel,
  LocalDateTime cancelTime
) {
}
