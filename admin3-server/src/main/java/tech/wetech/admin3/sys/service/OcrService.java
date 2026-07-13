package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;
import tech.wetech.admin3.sys.repository.ReimbursementExpenseItemRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OcrService {

  private static final Logger log = LoggerFactory.getLogger(OcrService.class);
  private static final String OCR_SERVER_URL = "http://localhost:8765";

  private final AttachmentRepository attachmentRepository;
  private final ReimbursementRepository reimbursementRepository;
  private final InvoiceRepository invoiceRepository;
  private final ReimbursementExpenseItemRepository expenseItemRepository;
  private final ObjectMapper objectMapper;

  public OcrService(AttachmentRepository attachmentRepository, ReimbursementRepository reimbursementRepository,
                    InvoiceRepository invoiceRepository, ReimbursementExpenseItemRepository expenseItemRepository,
                    ObjectMapper objectMapper) {
    this.attachmentRepository = attachmentRepository;
    this.reimbursementRepository = reimbursementRepository;
    this.invoiceRepository = invoiceRepository;
    this.expenseItemRepository = expenseItemRepository;
    this.objectMapper = objectMapper;
  }

  public void recognizeAttachmentAsync(Long attachmentId, Path imagePath) {
    Thread.startVirtualThread(() -> {
      try {
        recognizeAttachment(attachmentId, imagePath);
      } catch (Exception e) {
        log.error("OCR async recognition failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      }
    });
  }

  public void recognizeAttachment(Long attachmentId, Path imagePath) {
    Attachment attachment = attachmentRepository.findById(attachmentId).orElse(null);
    if (attachment == null) {
      log.warn("Attachment {} not found, skip OCR", attachmentId);
      return;
    }

    log.info("Starting OCR for attachment {}, file: {}", attachmentId, imagePath);
    attachment.setOcrStatus("processing");
    attachmentRepository.save(attachment);

    try {
      byte[] imageBytes = Files.readAllBytes(imagePath);
      String base64 = Base64.getEncoder().encodeToString(imageBytes);
      String ocrResultJson = callOcrService(base64, attachment.getFileName());
      log.info("OCR completed for attachment {}, result length: {}", attachmentId, ocrResultJson.length());

      attachment.setOcrStatus("completed");
      attachment.setOcrResult(ocrResultJson);
      attachmentRepository.save(attachment);

      updateReimbursementFromOcr(attachment, ocrResultJson);
    } catch (Exception e) {
      log.error("OCR failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      attachment.setOcrStatus("failed");
      String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      attachment.setOcrResult("{\"error\":\"" + errMsg.replace("\"", "'") + "\"}");
      attachmentRepository.save(attachment);
    }
  }

  private void updateReimbursementFromOcr(Attachment attachment, String ocrResultJson) {
    Long reimbursementId = attachment.getReimbursementId();
    if (reimbursementId == null) {
      log.debug("Attachment not linked to any reimbursement, skip auto-update");
      return;
    }
    Reimbursement r = reimbursementRepository.findById(reimbursementId).orElse(null);
    if (r == null) {
      log.warn("Reimbursement {} not found, skip auto-update", reimbursementId);
      return;
    }

    try {
      JsonNode data = unwrapOcrData(objectMapper.readTree(ocrResultJson));
      String ocrText = data.has("ocr_text") ? data.get("ocr_text").asText() : "";

      String invoiceNo = getJsonField(data, "invoice_no");
      String invoiceCode = getJsonField(data, "invoice_code");
      String totalAmount = getJsonField(data, "total_amount");
      String date = getJsonField(data, "date");
      String buyerName = getJsonField(data, "buyer_name");
      String sellerName = getJsonField(data, "seller_name");
      String buyerTaxId = getJsonField(data, "buyer_tax_id");
      String sellerTaxId = getJsonField(data, "seller_tax_id");
      String invoiceType = getJsonField(data, "invoice_type");
      String invoiceStatus = getJsonField(data, "invoice_status");
      String amountBeforeTax = getJsonField(data, "amount_before_tax");
      String taxRate = getJsonField(data, "tax_rate");
      String taxAmount = getJsonField(data, "tax_amount");
      String payee = getJsonField(data, "payee");
      String reviewer = getJsonField(data, "reviewer");
      String issuer = getJsonField(data, "issuer");
      String buyerAddressPhone = getJsonField(data, "buyer_address_phone");
      String buyerBankInfo = getJsonField(data, "buyer_bank_info");
      String sellerAddressPhone = getJsonField(data, "seller_address_phone");
      String sellerBankInfo = getJsonField(data, "seller_bank_info");
      String remarks = getJsonField(data, "remarks");

      if (!isEmpty(ocrText)) {
        if (isEmpty(invoiceNo)) invoiceNo = regexExtract(ocrText, "发票号码[：:\\s]*(\\d{8,20})");
        if (isEmpty(invoiceCode)) invoiceCode = regexExtract(ocrText, "发票代码[：:\\s]*(\\d{10,20})");
        if (isEmpty(date)) date = regexExtract(ocrText, "开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)");
        if (isEmpty(totalAmount)) {
          totalAmount = regexExtract(ocrText, "(?:价税合计|小写|合计)[\\s\\S]{0,20}[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)");
        }
      }

      LocalDateTime invoiceDate = parseInvoiceDate(date);
      boolean hasInvoiceData = hasInvoiceData(invoiceNo, invoiceCode, totalAmount, date, buyerName, sellerName,
        buyerTaxId, sellerTaxId, invoiceType, amountBeforeTax, taxAmount);
      Invoice invoice = invoiceRepository.findByAttachmentIdAndDeletedFalse(attachment.getId()).orElse(null);
      if (!hasInvoiceData) {
        if (invoice != null) {
          invoice.setDeleted(true);
          invoice.setDeletedAt(LocalDateTime.now());
          invoice.setUpdatedBy(attachment.getUploadedBy());
          invoice.setUpdatedAt(LocalDateTime.now());
          invoiceRepository.save(invoice);
        }
        if (!isEmpty(ocrText)) r.setDescription(ocrText);
        r.setOcrRawJson(ocrResultJson);
        updateInvoiceSummary(r);
        r.setUpdatedAt(LocalDateTime.now());
        reimbursementRepository.save(r);
        log.info("OCR attachment {} has no invoice fields, skipped formal invoice creation", attachment.getId());
        return;
      }
      if (invoice == null) {
        Invoice duplicate = findDuplicateInvoice(reimbursementId, invoiceNo, invoiceCode, attachment.getId());
        if (duplicate != null) {
          duplicate.setAttachmentId(duplicate.getAttachmentId() == null ? attachment.getId() : duplicate.getAttachmentId());
          duplicate.setUpdatedBy(attachment.getUploadedBy());
          duplicate.setUpdatedAt(LocalDateTime.now());
          Invoice savedDuplicate = invoiceRepository.save(duplicate);
          ReimbursementExpenseItem expenseItem = upsertExpenseItemFromInvoice(r, savedDuplicate, attachment, ocrText);
          if (expenseItem != null) {
            savedDuplicate.setExpenseItemId(expenseItem.getId());
            savedDuplicate.setUpdatedAt(LocalDateTime.now());
            invoiceRepository.save(savedDuplicate);
          }
          if (!isEmpty(ocrText)) r.setDescription(ocrText);
          r.setOcrRawJson(ocrResultJson);
          updateInvoiceSummary(r);
          r.setUpdatedAt(LocalDateTime.now());
          reimbursementRepository.save(r);
          log.info("OCR attachment {} matched duplicate invoice {}, skipped new invoice creation", attachment.getId(), savedDuplicate.getId());
          return;
        }
        invoice = new Invoice();
        invoice.setReimbursementId(reimbursementId);
        invoice.setAttachmentId(attachment.getId());
        invoice.setStatus("confirmed");
        invoice.setDeleted(false);
        invoice.setCreatedBy(attachment.getUploadedBy());
        invoice.setCreatedAt(LocalDateTime.now());
      }
      invoice.setImagePath(attachment.getFileUrl());
      invoice.setOcrRawJson(ocrResultJson);
      invoice.setInvoiceNo(invoiceNo);
      invoice.setInvoiceCode(invoiceCode);
      invoice.setInvoiceDate(invoiceDate);
      invoice.setBuyerName(buyerName);
      invoice.setSellerName(sellerName);
      invoice.setBuyerTaxId(buyerTaxId);
      invoice.setSellerTaxId(sellerTaxId);
      invoice.setTotalAmount(toBigDecimal(totalAmount));
      invoice.setInvoiceType(invoiceType);
      invoice.setInvoiceStatus(invoiceStatus);
      invoice.setAmountBeforeTax(toBigDecimal(amountBeforeTax));
      invoice.setTaxRate(taxRate);
      invoice.setTaxAmount(toBigDecimal(taxAmount));
      invoice.setPayee(payee);
      invoice.setReviewer(reviewer);
      invoice.setIssuer(issuer);
      invoice.setBuyerAddressPhone(buyerAddressPhone);
      invoice.setBuyerBankInfo(buyerBankInfo);
      invoice.setSellerAddressPhone(sellerAddressPhone);
      invoice.setSellerBankInfo(sellerBankInfo);
      invoice.setRemarks(remarks);
      invoice.setUpdatedBy(attachment.getUploadedBy());
      invoice.setUpdatedAt(LocalDateTime.now());
      Invoice savedInvoice = invoiceRepository.save(invoice);
      ReimbursementExpenseItem expenseItem = upsertExpenseItemFromInvoice(r, savedInvoice, attachment, ocrText);
      if (expenseItem != null) {
        savedInvoice.setExpenseItemId(expenseItem.getId());
        savedInvoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(savedInvoice);
      }

      if (!isEmpty(invoiceNo) && isEmpty(r.getInvoiceNo())) r.setInvoiceNo(invoiceNo);
      if (!isEmpty(invoiceCode) && isEmpty(r.getInvoiceCode())) r.setInvoiceCode(invoiceCode);
      if (invoiceDate != null && r.getInvoiceDate() == null) r.setInvoiceDate(invoiceDate);
      if (!isEmpty(buyerName) && isEmpty(r.getBuyerName())) r.setBuyerName(buyerName);
      if (!isEmpty(sellerName) && isEmpty(r.getSellerName())) r.setSellerName(sellerName);
      if (!isEmpty(buyerTaxId) && isEmpty(r.getBuyerTaxId())) r.setBuyerTaxId(buyerTaxId);
      if (!isEmpty(sellerTaxId) && isEmpty(r.getSellerTaxId())) r.setSellerTaxId(sellerTaxId);
      if (!isEmpty(invoiceType) && isEmpty(r.getInvoiceType())) r.setInvoiceType(invoiceType);
      if (!isEmpty(invoiceStatus) && isEmpty(r.getInvoiceStatus())) r.setInvoiceStatus(invoiceStatus);
      if (!isEmpty(ocrText)) r.setDescription(ocrText);
      r.setOcrRawJson(ocrResultJson);
      updateInvoiceSummary(r);
      r.setUpdatedAt(LocalDateTime.now());
      reimbursementRepository.save(r);

      log.info("Reimbursement {} updated from OCR attachment {}, invoiceNo={}, amount={}",
        reimbursementId, attachment.getId(), invoiceNo, totalAmount);
    } catch (Exception e) {
      log.error("Failed to auto-update reimbursement {} from OCR result: {}", reimbursementId, e.getMessage(), e);
    }
  }

  private JsonNode unwrapOcrData(JsonNode root) {
    JsonNode content = null;
    if (root.has("result") && root.get("result").has("content")) {
      content = root.get("result").get("content");
    } else if (root.has("content")) {
      content = root.get("content");
    }
    if (content != null && content.isArray() && !content.isEmpty() && content.get(0).has("text")) {
      try {
        return objectMapper.readTree(content.get(0).get("text").asText());
      } catch (Exception ignored) {
        return root;
      }
    }
    return root;
  }

  private String getJsonField(JsonNode node, String field) {
    if (node.has("invoice_fields") && node.get("invoice_fields").has(field)) {
      JsonNode value = node.get("invoice_fields").get(field);
      return value == null || value.isNull() ? "" : value.asText();
    }
    if (node.has(field)) {
      JsonNode value = node.get(field);
      return value == null || value.isNull() ? "" : value.asText();
    }
    return "";
  }

  private LocalDateTime parseInvoiceDate(String date) {
    if (isEmpty(date)) return null;
    List<DateTimeFormatter> formatters = List.of(
      DateTimeFormatter.ofPattern("yyyy年M月d日"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );
    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDate.parse(date, formatter).atStartOfDay();
      } catch (DateTimeParseException ignored) {}
    }
    try {
      return LocalDateTime.parse(date);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private BigDecimal toBigDecimal(String value) {
    if (isEmpty(value)) return null;
    String normalized = value.replace("¥", "").replace("￥", "").replace(",", "").trim();
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private void updateInvoiceSummary(Reimbursement r) {
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(r.getId());
    BigDecimal total = invoices.stream()
      .map(Invoice::getTotalAmount)
      .filter(amount -> amount != null)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    r.setInvoiceCount(invoices.size());
    r.setInvoiceTotalAmount(total);
  }

  private ReimbursementExpenseItem upsertExpenseItemFromInvoice(Reimbursement reimbursement, Invoice invoice,
                                                                Attachment attachment, String ocrText) {
    BigDecimal amount = invoice.getTotalAmount();
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return null;
    LocalDateTime now = LocalDateTime.now();
    String expenseType = classifyExpenseType(invoice, ocrText);
    ReimbursementExpenseItem item = expenseItemRepository.findByInvoiceId(invoice.getId()).orElse(null);
    if (item == null) {
      item = expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursement.getId()).stream()
        .filter(existing -> expenseType.equals(existing.getExpenseType())
          && "ocr".equals(existing.getSource())
          && sameExpenseDate(existing.getExpenseDate(), invoice.getInvoiceDate()))
        .findFirst()
        .orElse(null);
      if (item == null) {
        item = new ReimbursementExpenseItem();
        item.setReimbursementId(reimbursement.getId());
        item.setCreatedBy(attachment.getUploadedBy());
        item.setCreatedAt(now);
        int nextOrder = expenseItemRepository
          .findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursement.getId()).size() + 1;
        item.setSortOrder(nextOrder);
      }
    }
    item.setExpenseDate(invoice.getInvoiceDate());
    item.setExpenseType(expenseType);
    item.setOaExpenseName(oaExpenseName(expenseType));
    item.setRemark(invoice.getSellerName());
    item.setDescription(firstNonBlank(invoice.getRemarks(), invoice.getSellerName(), ocrText));
    item.setSource("ocr");
    item.setUpdatedBy(attachment.getUploadedBy());
    item.setUpdatedAt(now);
    ReimbursementExpenseItem saved = expenseItemRepository.save(item);
    invoice.setExpenseItemId(saved.getId());
    BigDecimal total = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursement.getId()).stream()
      .filter(existing -> saved.getId().equals(existing.getExpenseItemId()) || existing.getId().equals(invoice.getId()))
      .map(Invoice::getTotalAmount)
      .filter(value -> value != null)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    saved.setAmount(total);
    return expenseItemRepository.save(saved);
  }

  private Invoice findDuplicateInvoice(Long reimbursementId, String invoiceNo, String invoiceCode, Long attachmentId) {
    if (isEmpty(invoiceNo) || isEmpty(invoiceCode)) return null;
    return invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId).stream()
      .filter(invoice -> invoiceNo.equals(invoice.getInvoiceNo()) && invoiceCode.equals(invoice.getInvoiceCode()))
      .filter(invoice -> attachmentId == null || !attachmentId.equals(invoice.getAttachmentId()))
      .findFirst()
      .orElse(null);
  }

  private String classifyExpenseType(Invoice invoice, String ocrText) {
    String text = (firstNonBlank(invoice.getRemarks(), invoice.getSellerName(), invoice.getInvoiceContentShortName(), ocrText)).toLowerCase();
    if (containsAny(text, "加油", "汽油", "柴油", "石化", "石油", "油品")) return "fuel";
    if (containsAny(text, "高速", "通行费", "过路", "etc")) return "toll";
    if (containsAny(text, "高铁", "动车", "火车", "铁路")) return "rail";
    if (containsAny(text, "地铁", "轨道交通")) return "metro";
    if (containsAny(text, "网约车", "滴滴", "出行", "客运服务")) return "ride_hailing";
    if (containsAny(text, "顺风车")) return "carpool";
    if (containsAny(text, "货拉拉", "货运", "物流", "运输")) return "freight";
    if (containsAny(text, "停车")) return "parking";
    if (containsAny(text, "餐", "饭店", "餐饮", "酒楼", "美食")) return "meal";
    return "other";
  }

  private boolean sameExpenseDate(LocalDateTime left, LocalDateTime right) {
    if (left == null || right == null) return left == null && right == null;
    return left.toLocalDate().equals(right.toLocalDate());
  }

  private boolean containsAny(String text, String... keywords) {
    if (text == null) return false;
    for (String keyword : keywords) {
      if (text.contains(keyword.toLowerCase())) return true;
    }
    return false;
  }

  private String oaExpenseName(String expenseType) {
    return switch (expenseType == null ? "" : expenseType) {
      case "fuel" -> "加油费";
      case "toll" -> "高速过路费";
      case "rail" -> "高铁/动车/火车费";
      case "metro" -> "地铁费";
      case "ride_hailing" -> "网约车费";
      case "carpool" -> "顺风车费";
      case "freight" -> "货拉拉货运费";
      case "parking" -> "停车费";
      case "meal" -> "餐费";
      default -> "其他";
    };
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  private boolean hasInvoiceData(String... values) {
    for (String value : values) {
      if (!isEmpty(value) && !"0".equals(value.trim()) && !"0.00".equals(value.trim())) {
        return true;
      }
    }
    return false;
  }

  private String regexExtract(String text, String pattern) {
    var m = Pattern.compile(pattern).matcher(text);
    return m.find() ? m.group(1) : "";
  }

  private boolean isEmpty(String s) {
    return s == null || s.isBlank();
  }

  private String callOcrService(String imageBase64, String filename) throws Exception {
    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
    Path tempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "reimbursement-ocr");
    Files.createDirectories(tempDir);
    Path tempFile = tempDir.resolve(java.util.UUID.randomUUID() + "_" + filename);
    Files.write(tempFile, imageBytes);
    log.info("OCR temp file saved: {}", tempFile);

    try {
      String boundary = "----" + java.util.UUID.randomUUID();
      String lineSeparator = "\r\n";
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      baos.write(("--" + boundary + lineSeparator).getBytes());
      baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + lineSeparator).getBytes());
      baos.write(("Content-Type: application/octet-stream" + lineSeparator).getBytes());
      baos.write(lineSeparator.getBytes());
      baos.write(imageBytes);
      baos.write(lineSeparator.getBytes());
      baos.write(("--" + boundary + "--" + lineSeparator).getBytes());

      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(OCR_SERVER_URL + "/ocr"))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
        .build();

      java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("OCR service returned status " + response.statusCode() + ", body: " + response.body());
      }
      return response.body();
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (java.io.IOException e) {
        log.warn("Failed to delete OCR temp file: {}", tempFile);
      }
    }
  }
}
