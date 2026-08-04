package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tech.wetech.admin3.common.BusinessException;
import tech.wetech.admin3.common.CommonResultStatus;
import tech.wetech.admin3.common.SessionItemHolder;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.IncomingDocument;
import tech.wetech.admin3.sys.model.IncomingScanAttachment;
import tech.wetech.admin3.sys.model.OcrMappingConfig;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.IncomingDocumentRepository;
import tech.wetech.admin3.sys.repository.IncomingScanAttachmentRepository;
import tech.wetech.admin3.sys.repository.OcrMappingConfigRepository;
import tech.wetech.admin3.sys.service.dto.IncomingDocumentDTO;
import tech.wetech.admin3.sys.service.dto.PageDTO;
import tech.wetech.admin3.sys.service.dto.UserinfoDTO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static tech.wetech.admin3.common.CommonResultStatus.PARAM_ERROR;
import static tech.wetech.admin3.common.CommonResultStatus.RECORD_NOT_EXIST;
import static tech.wetech.admin3.common.Constants.SESSION_CURRENT_USER;

@Service
@Transactional
public class IncomingDocumentService {

  private static final String MODULE = "incoming-document";
  private static final String OCR_URL = "http://120.234.136.122:18769/ocr";

  private final IncomingDocumentRepository documentRepository;
  private final IncomingScanAttachmentRepository scanRepository;
  private final AttachmentRepository attachmentRepository;
  private final OcrMappingConfigRepository mappingConfigRepository;
  private final ObjectMapper objectMapper;
  private final IncomingDocumentOcrMapper ocrMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final Path uploadDir = Paths.get("uploads/attachments");

  public IncomingDocumentService(IncomingDocumentRepository documentRepository,
                                 IncomingScanAttachmentRepository scanRepository,
                                 AttachmentRepository attachmentRepository,
                                 OcrMappingConfigRepository mappingConfigRepository,
                                 ObjectMapper objectMapper) {
    this.documentRepository = documentRepository;
    this.scanRepository = scanRepository;
    this.attachmentRepository = attachmentRepository;
    this.mappingConfigRepository = mappingConfigRepository;
    this.objectMapper = objectMapper;
    this.ocrMapper = new IncomingDocumentOcrMapper(objectMapper);
  }

  public PageDTO<IncomingDocumentDTO> findDocuments(String keyword, Pageable pageable) {
    Page<IncomingDocument> page = documentRepository.search(keyword, pageable);
    return new PageDTO<>(page.map(IncomingDocumentDTO::brief).getContent(), page.getTotalElements());
  }

  public IncomingDocumentDTO findDocumentById(Long id) {
    IncomingDocument document = documentRepository.findById(id).orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    List<Attachment> attachments = attachmentRepository.findByIncomingDocumentIdAndDeletedFalseOrderByCreatedAtAsc(id).stream()
      .filter(item -> !"scan".equals(item.getAttachmentRole()))
      .toList();
    List<IncomingDocumentDTO.ScanAttachmentView> scans = scanViews(id);
    return IncomingDocumentDTO.of(document, attachments, scans);
  }

  public IncomingDocumentDTO createDocument(DocumentRequest request) {
    validateDocument(request, null);
    UserinfoDTO userInfo = currentUser();
    LocalDateTime now = LocalDateTime.now();
    IncomingDocument document = new IncomingDocument();
    fillDocument(document, request);
    document.setCode(firstNonBlank(request.code(), generateCode()));
    document.setCreatedAt(now);
    document.setCreatedBy(userInfo.userId());
    document.setUpdatedAt(now);
    document.setUpdatedBy(userInfo.userId());
    IncomingDocument saved = documentRepository.save(document);
    linkAttachments(saved.getId(), request.attachmentIds());
    return findDocumentById(saved.getId());
  }

  public IncomingDocumentDTO updateDocument(Long id, DocumentRequest request) {
    validateDocument(request, id);
    UserinfoDTO userInfo = currentUser();
    IncomingDocument document = documentRepository.findById(id).orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    fillDocument(document, request);
    document.setUpdatedAt(LocalDateTime.now());
    document.setUpdatedBy(userInfo.userId());
    documentRepository.save(document);
    if (request.attachmentIds() != null) {
      for (Attachment attachment : attachmentRepository.findByIncomingDocumentIdAndDeletedFalseOrderByCreatedAtAsc(id)) {
        if (!"scan".equals(attachment.getAttachmentRole())) attachment.setIncomingDocumentId(null);
      }
      linkAttachments(id, request.attachmentIds());
    }
    return findDocumentById(id);
  }

  public void deleteDocument(Long id) {
    documentRepository.delete(documentRepository.findById(id).orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST)));
    attachmentRepository.deleteByIncomingDocumentId(id);
    scanRepository.findByIncomingDocumentIdOrderByCreatedAtAsc(id).forEach(scanRepository::delete);
  }

  public IncomingDocumentDTO uploadAttachment(Long id, MultipartFile file) {
    ensureDocument(id);
    Attachment attachment = saveAttachment(file, id, "normal");
    attachmentRepository.save(attachment);
    return findDocumentById(id);
  }

  public IncomingDocumentDTO deleteAttachment(Long documentId, Long attachmentId) {
    ensureDocument(documentId);
    Attachment attachment = attachmentRepository.findById(attachmentId)
      .filter(item -> documentId.equals(item.getIncomingDocumentId()))
      .filter(item -> !"scan".equals(item.getAttachmentRole()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    attachment.setDeleted(true);
    attachment.setDeletedAt(LocalDateTime.now());
    attachmentRepository.save(attachment);
    return findDocumentById(documentId);
  }

  public IncomingDocumentDTO uploadScan(Long id, MultipartFile file) {
    ensureDocument(id);
    Attachment attachment = attachmentRepository.save(saveAttachment(file, id, "scan"));
    IncomingScanAttachment scan = new IncomingScanAttachment();
    scan.setIncomingDocumentId(id);
    scan.setAttachmentId(attachment.getId());
    scan.setOcrStatus("pending");
    scan.setCreatedAt(LocalDateTime.now());
    scan.setCreatedBy(currentUser().userId());
    scanRepository.save(scan);
    return recognizeScan(id, scan.getId());
  }

  public IncomingDocumentDTO recognizeScan(Long documentId, Long scanId) {
    ensureDocument(documentId);
    IncomingScanAttachment scan = scanRepository.findById(scanId)
      .filter(item -> documentId.equals(item.getIncomingDocumentId()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    Attachment attachment = attachmentRepository.findById(scan.getAttachmentId()).orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    scan.setOcrStatus("processing");
    scanRepository.save(scan);
    try {
      String raw = callOcr(storedPath(attachment), attachment.getFileName(), attachment.getFileType());
      Map<String, String> fields = ocrMapper.extractBusinessFields(raw);
      scan.setOcrRawJson(raw);
      scan.setOcrBusinessJson(objectMapper.writeValueAsString(fields));
      scan.setOcrStatus("completed");
    } catch (Exception e) {
      scan.setOcrStatus("failed");
      scan.setOcrRawJson("{\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
    }
    scanRepository.save(scan);
    return findDocumentById(documentId);
  }

  public IncomingDocumentDTO deleteScan(Long documentId, Long scanId) {
    IncomingScanAttachment scan = scanRepository.findById(scanId)
      .filter(item -> documentId.equals(item.getIncomingDocumentId()))
      .orElseThrow(() -> new BusinessException(RECORD_NOT_EXIST));
    scan.setDeleted(true);
    scan.setDeletedAt(LocalDateTime.now());
    scanRepository.save(scan);
    attachmentRepository.findById(scan.getAttachmentId()).ifPresent(attachment -> {
      attachment.setDeleted(true);
      attachment.setDeletedAt(LocalDateTime.now());
      attachmentRepository.save(attachment);
    });
    return findDocumentById(documentId);
  }

  public String findMapping() {
    Long userId = currentUser().userId();
    return mappingConfigRepository.findFirstByModuleAndUserIdOrderByUpdatedAtDesc(MODULE, userId)
      .map(OcrMappingConfig::getMappingJson)
      .orElse("[]");
  }

  public String saveMapping(String mappingJson) {
    Long userId = currentUser().userId();
    OcrMappingConfig config = mappingConfigRepository.findFirstByModuleAndUserIdOrderByUpdatedAtDesc(MODULE, userId)
      .orElseGet(OcrMappingConfig::new);
    config.setModule(MODULE);
    config.setUserId(userId);
    config.setMappingJson(mappingJson == null || mappingJson.isBlank() ? "[]" : mappingJson);
    config.setUpdatedAt(LocalDateTime.now());
    mappingConfigRepository.save(config);
    return config.getMappingJson();
  }

  private void validateDocument(DocumentRequest request, Long id) {
    if (request.title() == null || request.title().isBlank()) throw new BusinessException(PARAM_ERROR, "文件标题不能为空");
    if (request.code() != null && !request.code().isBlank()) {
      boolean exists = id == null ? documentRepository.existsByCode(request.code()) : documentRepository.existsByCodeAndIdNot(request.code(), id);
      if (exists) throw new BusinessException(PARAM_ERROR, "编号已存在");
    }
  }

  private void fillDocument(IncomingDocument document, DocumentRequest request) {
    if (request.code() != null && !request.code().isBlank()) document.setCode(request.code());
    document.setCarrierNo(request.carrierNo());
    document.setTitle(request.title());
    document.setDocNo(request.docNo());
    document.setSecrecyLevel(request.secrecyLevel());
    document.setUrgencyLevel(request.urgencyLevel());
    document.setHandler(request.handler());
    document.setRemark(request.remark());
  }

  private void linkAttachments(Long documentId, List<Long> attachmentIds) {
    if (attachmentIds == null) return;
    for (Long attachmentId : attachmentIds) {
      attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
        attachment.setIncomingDocumentId(documentId);
        attachmentRepository.save(attachment);
      });
    }
  }

  private Attachment saveAttachment(MultipartFile file, Long documentId, String role) {
    validateFile(file);
    try {
      Files.createDirectories(uploadDir);
      String originalName = file.getOriginalFilename();
      String ext = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
      String storedName = UUID.randomUUID() + ext;
      Files.copy(file.getInputStream(), uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
      Attachment attachment = new Attachment();
      attachment.setIncomingDocumentId(documentId);
      attachment.setFileName(originalName);
      attachment.setFileSize(file.getSize());
      attachment.setFileType(firstNonBlank(file.getContentType(), "application/octet-stream"));
      attachment.setFileUrl("/attachments/" + storedName + "/download");
      attachment.setAttachmentRole(role);
      attachment.setUploadedBy(currentUser().userId());
      attachment.setCreatedAt(LocalDateTime.now());
      attachment.setDeleted(false);
      attachment.setOcrStatus("scan".equals(role) ? "pending" : null);
      return attachment;
    } catch (IOException e) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "文件上传失败: " + e.getMessage());
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new BusinessException(PARAM_ERROR, "文件不能为空");
    if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException(PARAM_ERROR, "文件大小不能超过10MB");
  }

  private List<IncomingDocumentDTO.ScanAttachmentView> scanViews(Long documentId) {
    List<IncomingDocumentDTO.ScanAttachmentView> views = new ArrayList<>();
    for (IncomingScanAttachment scan : scanRepository.findByIncomingDocumentIdAndDeletedFalseOrderByCreatedAtAsc(documentId)) {
      attachmentRepository.findById(scan.getAttachmentId()).ifPresent(attachment -> views.add(new IncomingDocumentDTO.ScanAttachmentView(scan, attachment)));
    }
    return views;
  }

  private String callOcr(Path file, String filename, String contentType) throws Exception {
    String boundary = "----Admin3IncomingOcr" + UUID.randomUUID();
    byte[] fileBytes = Files.readAllBytes(file);
    byte[] body = multipartBody(boundary, filename, contentType, fileBytes);
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(OCR_URL))
      .header("Content-Type", "multipart/form-data; boundary=" + boundary)
      .POST(HttpRequest.BodyPublishers.ofByteArray(body))
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new BusinessException(CommonResultStatus.SERVER_ERROR, "OCR服务返回HTTP " + response.statusCode());
    }
    return response.body();
  }

  private byte[] multipartBody(String boundary, String filename, String contentType, byte[] fileBytes) throws IOException {
    String header = "--" + boundary + "\r\n"
      + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
      + "Content-Type: " + firstNonBlank(contentType, "application/octet-stream") + "\r\n\r\n";
    String footer = "\r\n--" + boundary + "--\r\n";
    byte[] headerBytes = header.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] footerBytes = footer.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
    System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
    System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
    System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
    return body;
  }

  private Path storedPath(Attachment attachment) {
    String url = attachment.getFileUrl();
    String storedName = url.substring("/attachments/".length(), url.indexOf("/download"));
    return uploadDir.resolve(storedName);
  }

  private void ensureDocument(Long id) {
    if (!documentRepository.existsById(id)) throw new BusinessException(RECORD_NOT_EXIST);
  }

  private UserinfoDTO currentUser() {
    return (UserinfoDTO) SessionItemHolder.getItem(SESSION_CURRENT_USER);
  }

  private String generateCode() {
    String prefix = "LW" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  public record DocumentRequest(
    String code,
    String carrierNo,
    String title,
    String docNo,
    String secrecyLevel,
    String urgencyLevel,
    String handler,
    String remark,
    List<Long> attachmentIds
  ) {}
}
