package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.service.LeaveService;
import tech.wetech.admin3.sys.service.dto.LeaveDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;

import java.time.LocalDateTime;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/leaves")
public class LeaveController {

  private final LeaveService leaveService;

  public LeaveController(LeaveService leaveService) {
    this.leaveService = leaveService;
  }

  @RequiresPermissions("leave:view")
  @GetMapping
  public ResponseEntity<PageDTO<LeaveDTO>> findLeaves(Pageable pageable) {
    return ResponseEntity.ok(leaveService.findLeaves(pageable));
  }

  @RequiresPermissions("leave:view")
  @GetMapping("/my")
  public ResponseEntity<PageDTO<LeaveDTO>> findMyLeaves(Pageable pageable) {
    return ResponseEntity.ok(leaveService.findMyLeaves(pageable));
  }

  @RequiresPermissions("leave:view")
  @GetMapping("/{leaveId}")
  public ResponseEntity<LeaveDTO> findLeaveById(@PathVariable Long leaveId) {
    return ResponseEntity.ok(leaveService.findLeaveById(leaveId));
  }

  @RequiresPermissions("leave:create")
  @PostMapping
  public ResponseEntity<LeaveDTO> createLeave(@RequestBody @Valid CreateLeaveRequest request) {
    return new ResponseEntity<>(leaveService.createLeave(request.leaveType(), request.startTime(), request.endTime(), request.leaveReason()), HttpStatus.CREATED);
  }

  @RequiresPermissions("leave:update")
  @PutMapping("/{leaveId}")
  public ResponseEntity<LeaveDTO> updateLeave(@PathVariable Long leaveId, @RequestBody @Valid CreateLeaveRequest request) {
    return ResponseEntity.ok(leaveService.updateLeave(leaveId, request.leaveType(), request.startTime(), request.endTime(), request.leaveReason()));
  }

  @RequiresPermissions("leave:approve")
  @PostMapping("/{leaveId}:approve")
  public ResponseEntity<LeaveDTO> approveLeave(@PathVariable Long leaveId) {
    return ResponseEntity.ok(leaveService.approveLeave(leaveId));
  }

  @RequiresPermissions("leave:approve")
  @PostMapping("/{leaveId}:reject")
  public ResponseEntity<LeaveDTO> rejectLeave(@PathVariable Long leaveId) {
    return ResponseEntity.ok(leaveService.rejectLeave(leaveId));
  }

  @RequiresPermissions("leave:cancel")
  @PostMapping("/{leaveId}:cancel")
  public ResponseEntity<LeaveDTO> cancelLeave(@PathVariable Long leaveId) {
    return ResponseEntity.ok(leaveService.cancelLeave(leaveId));
  }

  @RequiresPermissions("leave:delete")
  @DeleteMapping("/{leaveId}")
  public ResponseEntity<Void> deleteLeave(@PathVariable Long leaveId) {
    leaveService.deleteLeave(leaveId);
    return ResponseEntity.noContent().build();
  }

  record CreateLeaveRequest(@NotBlank String leaveType,
                            @NotNull LocalDateTime startTime,
                            @NotNull LocalDateTime endTime,
                            String leaveReason) {
  }

}
