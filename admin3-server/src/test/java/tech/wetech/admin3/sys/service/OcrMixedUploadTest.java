package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OcrMixedUploadTest {
  @Test
  void filenameMetadataDoesNotClassifyOrdinaryInvoiceAsItinerary() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    OcrService service = new OcrService(attachments, null, null, null, new ObjectMapper());
    Attachment attachment = new Attachment();
    attachment.setAttachmentRole("invoice");
    service.applyRecognizedAttachment(attachment, """
        {"input":{"filename":"行程单.pdf"},
         "pages":[{"lines":[{"text":"电子普通发票"}]}]}
        """, true);
    assertThat(attachment.getAttachmentRole()).isEqualTo("invoice");
    assertThat(attachment.getOcrStatus()).isEqualTo("completed");
  }

  @Test
  void extractsRecognizedLinesFromEveryPageAndBboxResults() throws Exception {
    OcrService service = new OcrService(null, null, null, null, new ObjectMapper());
    String text = service.extractOcrText(new ObjectMapper().readTree("""
        {"ocr_text":"首页","pages":[{"lines":[{"text":"起点"}]},
          {"lines":[{"text":"行程单"}]}],"ocr_raw":[{"text":"终点"}]}
        """));
    assertThat(text).contains("首页", "起点", "行程单", "终点");
  }

  @Test
  void paddlePagesLinesItineraryIsDeferredEvenWithoutOcrText() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    OcrService service = new OcrService(attachments, null, invoices, null, new ObjectMapper());
    Attachment attachment = new Attachment();
    attachment.setAttachmentRole("invoice");
    attachment.setReimbursementId(88L);
    service.applyRecognizedAttachment(attachment, """
        {"engine":"paddleocr","pages":[{"lines":[
          {"text":"滴滴出行"},{"text":"滴滴顺风车行程单"},
          {"text":"申请日期：2026-05-07"}]}],
          "invoice_fields":{"date":"2026-05-07"}}
        """, true);
    assertThat(attachment.getAttachmentRole()).isEqualTo("support");
    assertThat(attachment.getOcrStatus()).isEqualTo("processing");
    verifyNoInteractions(invoices);
  }

  @Test
  void itineraryWithInvoiceFieldsIsDeferredWithoutCreatingInvoice() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    OcrService service = new OcrService(attachments, null, invoices, null, new ObjectMapper());
    Attachment attachment = new Attachment();
    attachment.setAttachmentRole("invoice");
    attachment.setReimbursementId(87L);
    service.applyRecognizedAttachment(attachment,
        "{\"ocr_text\":\"滴滴顺风车行程单\",\"invoice_fields\":{\"date\":\"2026-10-09\",\"total_amount\":\"100.00\"}}", true);
    assertThat(attachment.getAttachmentRole()).isEqualTo("support");
    assertThat(attachment.getOcrStatus()).isEqualTo("processing");
    verify(attachments).save(attachment);
    verifyNoInteractions(invoices);
  }

  @Test
  void wrappedOcrTextAlsoClassifiesItinerary() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    OcrService service = new OcrService(attachments, null, null, null, new ObjectMapper());
    Attachment attachment = new Attachment();
    attachment.setAttachmentRole("invoice");
    String payload = new ObjectMapper().writeValueAsString(java.util.Map.of("content",
        java.util.List.of(java.util.Map.of("text", "{\"ocr_text\":\"用车行程单\"}"))));
    service.applyRecognizedAttachment(attachment, payload, true);
    assertThat(attachment.getAttachmentRole()).isEqualTo("support");
    assertThat(attachment.getOcrResult()).isEqualTo(payload);
  }

  @Test
  void ordinaryInvoiceKeepsOriginalRoleAndCompletes() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    OcrService service = new OcrService(attachments, null, null, null, new ObjectMapper());
    Attachment attachment = new Attachment();
    attachment.setAttachmentRole("invoice");
    service.applyRecognizedAttachment(attachment, "{\"ocr_text\":\"电子普通发票\"}", true);
    assertThat(attachment.getAttachmentRole()).isEqualTo("invoice");
    assertThat(attachment.getOcrStatus()).isEqualTo("completed");
  }

  @Test
  void supportIsAppliedOnlyAfterAllFilesIncludingFailuresHaveFinished() {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    OcrService service = spy(new OcrService(attachments, null, null, null, new ObjectMapper()));
    Attachment itinerary = new Attachment();
    itinerary.setAttachmentRole("support");
    itinerary.setOcrStatus("processing");
    Attachment invoice = new Attachment();
    invoice.setAttachmentRole("invoice");
    invoice.setOcrStatus("completed");
    Attachment failed = new Attachment();
    failed.setAttachmentRole("invoice");
    failed.setOcrStatus("failed");
    when(attachments.findById(1L)).thenReturn(Optional.of(itinerary));
    when(attachments.findById(2L)).thenReturn(Optional.of(invoice));
    when(attachments.findById(3L)).thenReturn(Optional.of(failed));
    doNothing().when(service).recognizeAttachment(anyLong(), any(Path.class), eq(true));
    doNothing().when(service).applySupportAttachmentOcr(1L);
    var files = new LinkedHashMap<Long, Path>();
    files.put(1L, Path.of("trip.pdf"));
    files.put(2L, Path.of("invoice.pdf"));
    files.put(3L, Path.of("failed.pdf"));
    service.recognizeAttachmentBatch(files);
    var order = inOrder(service);
    order.verify(service).recognizeAttachment(1L, Path.of("trip.pdf"), true);
    order.verify(service).recognizeAttachment(2L, Path.of("invoice.pdf"), true);
    order.verify(service).recognizeAttachment(3L, Path.of("failed.pdf"), true);
    order.verify(service).applySupportAttachmentOcr(1L);
    assertThat(itinerary.getOcrStatus()).isEqualTo("completed");
    assertThat(failed.getOcrStatus()).isEqualTo("failed");
    verify(service, never()).applySupportAttachmentOcr(2L);
  }

  @Test
  void deferredItineraryUsesExistingInvoiceNumberAssociation() throws Exception {
    AttachmentRepository attachments = mock(AttachmentRepository.class);
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    OcrService service = spy(new OcrService(attachments, null, invoices, null, new ObjectMapper()));
    Attachment itinerary = new Attachment();
    itinerary.setId(1L);
    itinerary.setReimbursementId(87L);
    itinerary.setAttachmentRole("invoice");
    itinerary.setOcrStatus("pending");
    Invoice invoice = new Invoice();
    invoice.setInvoiceNo("26447000000874304409");
    var invoiceReady = new java.util.concurrent.atomic.AtomicBoolean();
    when(attachments.findById(1L)).thenReturn(Optional.of(itinerary));
    when(attachments.findById(2L)).thenReturn(Optional.empty());
    when(invoices.findBySupportAttachmentIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
    when(invoices.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(87L)).thenAnswer(call -> {
      assertThat(invoiceReady.get()).isTrue();
      return java.util.List.of(invoice);
    });
    when(invoices.save(invoice)).thenReturn(invoice);
    doAnswer(call -> {
      service.applyRecognizedAttachment(itinerary,
          "{\"ocr_text\":\"滴滴行程单\",\"invoice_fields\":{\"invoice_no\":\"26447000000874304409\"}}", true);
      return null;
    }).when(service).recognizeAttachment(1L, Path.of("trip.pdf"), true);
    doAnswer(call -> { invoiceReady.set(true); return null; })
        .when(service).recognizeAttachment(2L, Path.of("invoice.pdf"), true);
    var files = new LinkedHashMap<Long, Path>();
    files.put(1L, Path.of("trip.pdf"));
    files.put(2L, Path.of("invoice.pdf"));
    service.recognizeAttachmentBatch(files);
    assertThat(invoice.getSupportAttachmentId()).isEqualTo(1L);
    assertThat(itinerary.getOcrStatus()).isEqualTo("completed");
    verify(invoices).save(invoice);
  }
}
