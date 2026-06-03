package tech.wetech.admin3.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.common.authz.RequiresPermissions;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import static tech.wetech.admin3.common.CommonResultStatus.PARAM_ERROR;
import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.CommonResultStatus.SERVER_ERROR;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/attachments")
public class AttachmentController {

  private final AttachmentRepository attachmentRepository;
  private final Path uploadDir = Paths.get("uploads/attachments");

  public AttachmentController(AttachmentRepository attachmentRepository) {
    this.attachmentRepository = attachmentRepository;
  }

  @RequiresPermissions("reimbursement:create")
  @PostMapping("/upload")
  public ResponseEntity<ReimbursementDTO.AttachmentDTO> uploadAttachment(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(PARAM_ERROR, "文件不能为空");
    }
    // 校验文件大小（10MB）
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new BusinessException(PARAM_ERROR, "文件大小不能超过10MB");
    }
    // 校验文件类型
    String contentType = file.getContentType();
    if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
      throw new BusinessException(PARAM_ERROR, "仅支持jpg/png/gif/webp/pdf格式");
    }

    try {
      Files.createDirectories(uploadDir);
      String originalName = file.getOriginalFilename();
      String ext = "";
      if (originalName != null && originalName.contains(".")) {
        ext = originalName.substring(originalName.lastIndexOf("."));
      }
      String storedName = UUID.randomUUID().toString() + ext;
      Path targetPath = uploadDir.resolve(storedName);
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

      UserinfoDTO userInfo = (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);

      Attachment attachment = new Attachment();
      attachment.setFileName(originalName);
      attachment.setFileSize(file.getSize());
      attachment.setFileType(contentType);
      attachment.setFileUrl("/attachments/" + storedName + "/download");
      attachment.setUploadedBy(userInfo.userId());
      attachment.setCreatedAt(LocalDateTime.now());
      attachment = attachmentRepository.save(attachment);

      return ResponseEntity.ok(new ReimbursementDTO.AttachmentDTO(attachment));
    } catch (IOException e) {
      throw new BusinessException(SERVER_ERROR, "文件上传失败: " + e.getMessage());
    }
  }

  @RequiresPermissions("reimbursement:delete")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
    Attachment attachment = attachmentRepository.findById(id)
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    // 删除物理文件
    try {
      String storedName = attachment.getFileUrl().substring(
        attachment.getFileUrl().lastIndexOf("/") + 1,
        attachment.getFileUrl().lastIndexOf("/download")
      );
      Path filePath = uploadDir.resolve(storedName);
      Files.deleteIfExists(filePath);
    } catch (IOException ignored) {
    }
    attachmentRepository.delete(attachment);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{storedName}/download")
  public ResponseEntity<Resource> downloadAttachment(@PathVariable String storedName) {
    try {
      Path filePath = uploadDir.resolve(storedName);
      Resource resource = new UrlResource(filePath.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new BusinessException(RECORD_NOT_EXIST, "文件不存在");
      }
      // 从数据库获取文件信息以确定 MIME 类型
      Attachment attachment = attachmentRepository.findByFileUrlEndingWith(storedName + "/download");
      String contentType = attachment != null ? attachment.getFileType() : "application/octet-stream";
      String fileName = attachment != null ? attachment.getFileName() : storedName;

      return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
        .body(resource);
    } catch (MalformedURLException e) {
      throw new BusinessException(RECORD_NOT_EXIST, "文件不存在");
    }
  }
}
