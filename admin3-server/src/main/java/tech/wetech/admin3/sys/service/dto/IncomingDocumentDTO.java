package tech.wetech.admin3.sys.service.dto;

import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.IncomingDocument;
import tech.wetech.admin3.sys.model.IncomingScanAttachment;

import java.time.LocalDateTime;
import java.util.List;

public record IncomingDocumentDTO(
  Long id,
  String code,
  String carrierNo,
  String title,
  String docNo,
  String secrecyLevel,
  String urgencyLevel,
  String handler,
  String remark,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  List<AttachmentDTO> attachments,
  List<ScanAttachmentDTO> scanAttachments
) {

  public static IncomingDocumentDTO brief(IncomingDocument d) {
    return of(d, List.of(), List.of());
  }

  public static IncomingDocumentDTO of(IncomingDocument d, List<Attachment> attachments, List<ScanAttachmentView> scans) {
    return new IncomingDocumentDTO(
      d.getId(),
      d.getCode(),
      d.getCarrierNo(),
      d.getTitle(),
      d.getDocNo(),
      d.getSecrecyLevel(),
      d.getUrgencyLevel(),
      d.getHandler(),
      d.getRemark(),
      d.getCreatedAt(),
      d.getUpdatedAt(),
      attachments.stream().map(AttachmentDTO::new).toList(),
      scans.stream().map(ScanAttachmentDTO::new).toList()
    );
  }

  public record AttachmentDTO(Long id, String fileName, Long fileSize, String fileType, String fileUrl, String attachmentRole, String ocrStatus, String ocrResult) {
    public AttachmentDTO(Attachment a) {
      this(a.getId(), a.getFileName(), a.getFileSize(), a.getFileType(), a.getFileUrl(), a.getAttachmentRole(), a.getOcrStatus(), a.getOcrResult());
    }
  }

  public record ScanAttachmentDTO(
    Long id,
    Long attachmentId,
    String fileName,
    Long fileSize,
    String fileType,
    String fileUrl,
    String ocrStatus,
    String ocrRawJson,
    String ocrBusinessJson,
    LocalDateTime createdAt
  ) {
    public ScanAttachmentDTO(ScanAttachmentView view) {
      this(
        view.scan().getId(),
        view.attachment().getId(),
        view.attachment().getFileName(),
        view.attachment().getFileSize(),
        view.attachment().getFileType(),
        view.attachment().getFileUrl(),
        view.scan().getOcrStatus(),
        view.scan().getOcrRawJson(),
        view.scan().getOcrBusinessJson(),
        view.scan().getCreatedAt()
      );
    }
  }

  public record ScanAttachmentView(IncomingScanAttachment scan, Attachment attachment) {}
}
