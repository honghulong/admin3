package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;
import tech.wetech.admin3.sys.repository.ReimbursementExpenseItemRepository;
import tech.wetech.admin3.sys.service.ocr.OcrExecutionService;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.imageio.ImageIO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class OcrService {

  private static final Logger log = LoggerFactory.getLogger(OcrService.class);
  private static final String OCR_SERVER_URL = "http://127.0.0.1:8769";
  private static final java.time.Duration OCR_REQUEST_TIMEOUT = java.time.Duration.ofSeconds(120);
  private static final Semaphore OCR_REQUESTS = new Semaphore(1, true);
  private static final List<ExpenseTypeRule> EXPENSE_TYPE_RULES = List.of(
    new ExpenseTypeRule("taxi", List.of("出租车", "的士", "taxi")),
    new ExpenseTypeRule("passenger_transport", List.of("汽车客运", "客运车票", "长途客运")),
    new ExpenseTypeRule("insurance", List.of("保险", "保费", "财产保险")),
    new ExpenseTypeRule("hotel", List.of("住宿", "酒店", "宾馆", "旅店")),
    new ExpenseTypeRule("air_travel", List.of("航空运输", "机票", "民航")),
    new ExpenseTypeRule("fuel", List.of("加油", "汽油", "柴油", "石化", "石油", "油品")),
    new ExpenseTypeRule("toll", List.of("高速", "通行费", "过路", "etc")),
    new ExpenseTypeRule("rail", List.of("高铁", "动车", "火车", "铁路")),
    new ExpenseTypeRule("metro", List.of("地铁", "轨道交通")),
    new ExpenseTypeRule("ride_hailing", List.of("网约车", "滴滴", "didi", "出行", "客运服务", "接机", "送机", "用车")),
    new ExpenseTypeRule("carpool", List.of("顺风车")),
    new ExpenseTypeRule("freight", List.of("货拉拉", "货运", "物流", "运输")),
    new ExpenseTypeRule("delivery", List.of("配送", "快递", "速运", "运费")),
    new ExpenseTypeRule("parking", List.of("停车")),
    new ExpenseTypeRule("meal", List.of("餐", "饭店", "餐饮", "酒楼", "美食"))
  );

  private final AttachmentRepository attachmentRepository;
  private final ReimbursementRepository reimbursementRepository;
  private final InvoiceRepository invoiceRepository;
  private final ReimbursementExpenseItemRepository expenseItemRepository;
  private final ObjectMapper objectMapper;
  private final OcrExecutionService ocrExecutionService;

  public OcrService(AttachmentRepository attachmentRepository, ReimbursementRepository reimbursementRepository,
                    InvoiceRepository invoiceRepository, ReimbursementExpenseItemRepository expenseItemRepository,
                    ObjectMapper objectMapper) {
    this(attachmentRepository, reimbursementRepository, invoiceRepository, expenseItemRepository, objectMapper, null);
  }

  @Autowired
  public OcrService(AttachmentRepository attachmentRepository, ReimbursementRepository reimbursementRepository,
                    InvoiceRepository invoiceRepository, ReimbursementExpenseItemRepository expenseItemRepository,
                    ObjectMapper objectMapper, OcrExecutionService ocrExecutionService) {
    this.attachmentRepository = attachmentRepository;
    this.reimbursementRepository = reimbursementRepository;
    this.invoiceRepository = invoiceRepository;
    this.expenseItemRepository = expenseItemRepository;
    this.objectMapper = objectMapper;
    this.ocrExecutionService = ocrExecutionService;
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

  public void recognizeInvoiceSupportAttachmentAsync(Long attachmentId, Path imagePath) {
    Thread.startVirtualThread(() -> {
      try {
        recognizeInvoiceSupportAttachment(attachmentId, imagePath);
      } catch (Exception e) {
        log.error("Support attachment OCR async recognition failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      }
    });
  }

  public void recognizeAttachment(Long attachmentId, Path imagePath) {
    recognizeAttachment(attachmentId, imagePath, false);
  }

  public void recognizeAttachmentBatchAsync(Map<Long, Path> files) {
    Thread.startVirtualThread(() -> recognizeAttachmentBatch(files));
  }

  void recognizeAttachmentBatch(Map<Long, Path> files) {
    // All ordinary invoices must be saved before deferred support attachments are linked.
    files.forEach((id, path) -> recognizeAttachment(id, path, true));
    for (Long id : files.keySet()) {
      Attachment attachment = attachmentRepository.findById(id).orElse(null);
      if (attachment == null || Boolean.TRUE.equals(attachment.getDeleted())
          || !"support".equals(attachment.getAttachmentRole())
          || !"processing".equals(attachment.getOcrStatus())) continue;
      applySupportAttachmentOcr(id);
      attachment.setOcrStatus("completed");
      attachmentRepository.save(attachment);
    }
  }

  void recognizeAttachment(Long attachmentId, Path imagePath, boolean deferSupport) {
    Attachment attachment = attachmentRepository.findById(attachmentId).orElse(null);
    if (attachment == null) {
      log.warn("Attachment {} not found, skip OCR", attachmentId);
      return;
    }

    log.info("Starting OCR for attachment {}, file: {}", attachmentId, imagePath);
    attachment.setOcrStatus("processing");
    attachmentRepository.save(attachment);

    try {
      String ocrResultJson = recognizeInvoiceFile(imagePath, attachment.getFileName());
      String safeOcrResultJson = stripSupplementaryChars(ocrResultJson);
      log.info("OCR completed for attachment {}, result length: {}", attachmentId, ocrResultJson.length());

      applyRecognizedAttachment(attachment, safeOcrResultJson, deferSupport);
    } catch (Exception e) {
      log.error("OCR failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      attachment.setOcrStatus("failed");
      String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      attachment.setOcrResult(stripSupplementaryChars("{\"error\":\"" + errMsg.replace("\"", "'") + "\"}"));
      attachmentRepository.save(attachment);
    }
  }

  void applyRecognizedAttachment(Attachment attachment, String ocrResultJson, boolean deferSupport) throws Exception {
    JsonNode data = unwrapOcrData(objectMapper.readTree(ocrResultJson));
    boolean itinerary = extractOcrText(data).contains("行程单");
    attachment.setOcrResult(ocrResultJson);
    if (itinerary) {
      attachment.setAttachmentRole("support");
      attachment.setOcrStatus(deferSupport ? "processing" : "completed");
      attachmentRepository.save(attachment);
      if (!deferSupport) updateExpenseItemTripFromSupportOcr(attachment, ocrResultJson);
      log.info("Attachment {} identified as itinerary, deferred={}", attachment.getId(), deferSupport);
      return;
    }
    updateReimbursementFromOcr(attachment, ocrResultJson);
    attachment.setOcrStatus("completed");
    attachmentRepository.save(attachment);
  }

  String extractOcrText(JsonNode data) {
    List<String> lines = new ArrayList<>();
    String text = data.path("ocr_text").asText("");
    if (!isEmpty(text)) lines.add(text);
    // PaddleOCR returns recognized text in pages[].lines[], without ocr_text.
    for (JsonNode page : data.path("pages")) {
      for (JsonNode line : page.path("lines")) {
        String value = line.path("text").asText("");
        if (!isEmpty(value)) lines.add(value);
      }
    }
    JsonNode raw = data.has("ocr_raw") ? data.path("ocr_raw") : data.path("raw").path("ocr_raw");
    for (JsonNode line : raw) {
      String value = line.path("text").asText("");
      if (!isEmpty(value)) lines.add(value);
    }
    return String.join("\n", lines);
  }

  public void recognizeInvoiceSupportAttachment(Long attachmentId, Path imagePath) {
    Attachment attachment = attachmentRepository.findById(attachmentId).orElse(null);
    if (attachment == null) {
      log.warn("Support attachment {} not found, skip OCR", attachmentId);
      return;
    }

    log.info("Starting support attachment OCR for attachment {}, file: {}", attachmentId, imagePath);
    attachment.setOcrStatus("processing");
    attachmentRepository.save(attachment);

    try {
      byte[] imageBytes = Files.readAllBytes(imagePath);
      String base64 = Base64.getEncoder().encodeToString(imageBytes);
      String ocrResultJson = callOcrService(base64, attachment.getFileName());
      String safeOcrResultJson = stripSupplementaryChars(ocrResultJson);
      attachment.setOcrStatus("completed");
      attachment.setOcrResult(safeOcrResultJson);
      attachmentRepository.save(attachment);
      updateExpenseItemTripFromSupportOcr(attachment, safeOcrResultJson);
    } catch (Exception e) {
      log.error("Support attachment OCR failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      attachment.setOcrStatus("failed");
      String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      attachment.setOcrResult(stripSupplementaryChars("{\"error\":\"" + errMsg.replace("\"", "'") + "\"}"));
      attachmentRepository.save(attachment);
    }
  }

  public void applySupportAttachmentOcr(Long attachmentId) {
    Attachment attachment = attachmentRepository.findById(attachmentId).orElse(null);
    if (attachment == null || attachment.getOcrResult() == null) {
      return;
    }
    updateExpenseItemTripFromSupportOcr(attachment, attachment.getOcrResult());
  }

  private synchronized void updateReimbursementFromOcr(Attachment attachment, String ocrResultJson) {
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
      String ocrText = extractOcrText(data);

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
        String invoiceNoFromText = regexExtract(ocrText, "发票号码[：:\\s]*(\\d{8,20})");
        if (isEmpty(invoiceNoFromText)) invoiceNoFromText = regexExtract(ocrText, "(?<!\\d)(\\d{20})(?!\\d)");
        if (isBetterInvoiceNo(invoiceNoFromText, invoiceNo)) invoiceNo = invoiceNoFromText;
        if (isEmpty(invoiceCode)) invoiceCode = regexExtract(ocrText, "发票代码[：:\\s]*(\\d{10,20})");
        if (isEmpty(date)) date = regexExtract(ocrText, "开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)");
        if (isEmpty(date)) date = extractDateFromText(ocrText);
        if (isEmpty(totalAmount)) {
          totalAmount = extractTotalAmountFromText(ocrText);
        }
        if (isEmpty(buyerName)) buyerName = regexExtract(ocrText, "购买方信息[\\s\\S]{0,80}名称[：:]\\s*([^\\r\\n]+)");
        if (isEmpty(sellerName)) sellerName = regexExtract(ocrText, "销售方信息[\\s\\S]{0,80}名称[：:]\\s*([^\\r\\n]+)");
        if (isEmpty(sellerName)) sellerName = regexExtract(ocrText, "名称[：:]\\s*([^\\r\\n]*滴滴[^\\r\\n]*)");
        List<String> taxIds = regexExtractAll(ocrText, "统一社会信用代码/纳税人识别号[：:]\\s*([0-9A-Z]{12,30})");
        if (isEmpty(buyerTaxId) && !taxIds.isEmpty()) buyerTaxId = taxIds.get(0);
        if (isEmpty(sellerTaxId) && taxIds.size() > 1) sellerTaxId = taxIds.get(1);
        List<String> subtotalAmounts = extractMoneyBefore(ocrText, "出行人");
        if (isEmpty(amountBeforeTax) && subtotalAmounts.size() >= 2) amountBeforeTax = subtotalAmounts.get(subtotalAmounts.size() - 2);
        if (isEmpty(taxAmount) && subtotalAmounts.size() >= 1) taxAmount = subtotalAmounts.get(subtotalAmounts.size() - 1);
        List<String> summaryAmounts = extractSummaryAmountPair(ocrText);
        if (shouldUseSummaryAmountPair(totalAmount, summaryAmounts, amountBeforeTax, taxAmount)) {
          amountBeforeTax = summaryAmounts.get(0);
          taxAmount = summaryAmounts.get(1);
        }
        totalAmount = reconcileTotalAmountFromSummary(totalAmount, summaryAmounts);
        Matcher trailingCurrencySummary = Pattern.compile(
          "(\\d+\\.\\d{2})[\u00a5\uffe5?]\\s*(\\d+\\.\\d{2})[\u00a5\uffe5?][\\s\\S]{0,80}?[\u00a5\uffe5?]\\s*(\\d+\\.\\d{2})")
          .matcher(ocrText);
        if (trailingCurrencySummary.find()) {
          amountBeforeTax = trailingCurrencySummary.group(1);
          taxAmount = trailingCurrencySummary.group(2);
          totalAmount = trailingCurrencySummary.group(3);
        }
        Matcher lineAmountTax = Pattern.compile("(\\d+\\.\\d{2})\\s+\\1\\s+\\d+(?:\\.\\d+)?%\\s+(\\d+\\.\\d{2})")
          .matcher(ocrText);
        if (lineAmountTax.find()) {
          BigDecimal lineAmount = toBigDecimal(lineAmountTax.group(1));
          BigDecimal lineTax = toBigDecimal(lineAmountTax.group(2));
          BigDecimal expectedTotal = lineAmount.add(lineTax).setScale(2, java.math.RoundingMode.HALF_UP);
          if (sameMoney(toBigDecimal(totalAmount), expectedTotal)) {
            amountBeforeTax = lineAmount.toPlainString();
            taxAmount = lineTax.toPlainString();
          }
        }
        if (ocrText.contains("\u4e0d\u5f81\u7a0e")) {
          totalAmount = firstNonBlank(totalAmount, amountBeforeTax);
          amountBeforeTax = firstNonBlank(totalAmount, amountBeforeTax);
          taxAmount = "0.00";
          taxRate = "\u4e0d\u5f81\u7a0e";
        }
        InvoiceParties parties = extractInvoiceParties(ocrText);
        if (!isEmpty(parties.buyerName())) buyerName = parties.buyerName();
        if (!isEmpty(parties.sellerName())) sellerName = parties.sellerName();
        if (!isEmpty(parties.buyerTaxId())) buyerTaxId = parties.buyerTaxId();
        if (!isEmpty(parties.sellerTaxId())) sellerTaxId = parties.sellerTaxId();
        if (isEmpty(taxRate)) taxRate = regexExtract(ocrText, "(\\d+(?:\\.\\d+)?%)");
        if (isEmpty(issuer)) issuer = regexExtract(ocrText, "开票人[：:]\\s*([^\\r\\n]+)");
        if (isEmpty(invoiceType) && ocrText.contains("普通发票")) invoiceType = "普通发票";
        if (isEmpty(remarks)) remarks = extractRemarksFromText(ocrText);
      }

      if (isEmpty(date)) date = extractDateFromText(attachment.getFileName());

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
        r.setOcrRawJson(stripSupplementaryChars(ocrResultJson));
        updateInvoiceSummary(r);
        r.setUpdatedAt(LocalDateTime.now());
        reimbursementRepository.save(r);
        log.info("OCR attachment {} has no invoice fields, skipped formal invoice creation", attachment.getId());
        return;
      }
      if (invoice == null) {
        Invoice duplicate = findDuplicateInvoice(reimbursementId, invoiceNo, invoiceCode, invoiceDate, totalAmount, attachment.getId());
        if (duplicate != null) {
          duplicate.setAttachmentId(duplicate.getAttachmentId() == null ? attachment.getId() : duplicate.getAttachmentId());
          applyInvoiceOcrFields(duplicate, attachment, ocrResultJson, invoiceNo, invoiceCode, invoiceDate,
            buyerName, sellerName, buyerTaxId, sellerTaxId, totalAmount, invoiceType, invoiceStatus,
            amountBeforeTax, taxRate, taxAmount, payee, reviewer, issuer, buyerAddressPhone, buyerBankInfo,
            sellerAddressPhone, sellerBankInfo, remarks);
          duplicate.setUpdatedBy(attachment.getUploadedBy());
          duplicate.setUpdatedAt(LocalDateTime.now());
          Invoice savedDuplicate = invoiceRepository.save(duplicate);
          ReimbursementExpenseItem expenseItem = upsertExpenseItemFromInvoice(r, savedDuplicate, attachment, ocrText);
          if (expenseItem != null) {
            savedDuplicate.setExpenseItemId(expenseItem.getId());
            savedDuplicate.setUpdatedAt(LocalDateTime.now());
            invoiceRepository.save(savedDuplicate);
          }
          r.setOcrRawJson(stripSupplementaryChars(ocrResultJson));
          updateInvoiceSummary(r);
          r.setUpdatedAt(LocalDateTime.now());
          reimbursementRepository.save(r);
          syncExpenseItemDescriptionsFromInvoiceRemarks(reimbursementId, attachment.getUploadedBy());
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
      applyInvoiceOcrFields(invoice, attachment, ocrResultJson, invoiceNo, invoiceCode, invoiceDate,
        buyerName, sellerName, buyerTaxId, sellerTaxId, totalAmount, invoiceType, invoiceStatus,
        amountBeforeTax, taxRate, taxAmount, payee, reviewer, issuer, buyerAddressPhone, buyerBankInfo,
        sellerAddressPhone, sellerBankInfo, remarks);
      Invoice savedInvoice = invoiceRepository.save(invoice);
      ReimbursementExpenseItem expenseItem = upsertExpenseItemFromInvoice(r, savedInvoice, attachment, ocrText);
      if (expenseItem != null) {
        savedInvoice.setExpenseItemId(expenseItem.getId());
        savedInvoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(savedInvoice);
      }
      regroupExpenseItemsByInvoiceTrips(reimbursementId, attachment.getUploadedBy());
      syncExpenseItemDescriptionsFromInvoiceRemarks(reimbursementId, attachment.getUploadedBy());

      if (!isEmpty(invoiceNo) && isEmpty(r.getInvoiceNo())) r.setInvoiceNo(invoiceNo);
      if (!isEmpty(invoiceCode) && isEmpty(r.getInvoiceCode())) r.setInvoiceCode(invoiceCode);
      if (invoiceDate != null && r.getInvoiceDate() == null) r.setInvoiceDate(invoiceDate);
      if (!isEmpty(buyerName) && isEmpty(r.getBuyerName())) r.setBuyerName(buyerName);
      if (!isEmpty(sellerName) && isEmpty(r.getSellerName())) r.setSellerName(sellerName);
      if (!isEmpty(buyerTaxId) && isEmpty(r.getBuyerTaxId())) r.setBuyerTaxId(buyerTaxId);
      if (!isEmpty(sellerTaxId) && isEmpty(r.getSellerTaxId())) r.setSellerTaxId(sellerTaxId);
      if (!isEmpty(invoiceType) && isEmpty(r.getInvoiceType())) r.setInvoiceType(invoiceType);
      if (!isEmpty(invoiceStatus) && isEmpty(r.getInvoiceStatus())) r.setInvoiceStatus(invoiceStatus);
      r.setOcrRawJson(stripSupplementaryChars(ocrResultJson));
      updateInvoiceSummary(r);
      r.setUpdatedAt(LocalDateTime.now());
      reimbursementRepository.save(r);

      log.info("Reimbursement {} updated from OCR attachment {}, invoiceNo={}, amount={}",
        reimbursementId, attachment.getId(), invoiceNo, totalAmount);
    } catch (Exception e) {
      log.error("Failed to auto-update reimbursement {} from OCR result: {}", reimbursementId, e.getMessage(), e);
    }
  }

  private void updateExpenseItemTripFromSupportOcr(Attachment attachment, String ocrResultJson) {
    try {
      JsonNode data = unwrapOcrData(objectMapper.readTree(ocrResultJson));
      Invoice invoice = invoiceRepository.findBySupportAttachmentIdAndDeletedFalse(attachment.getId()).orElse(null);
      if (invoice == null) {
        invoice = linkSupportAttachmentByInvoiceNo(attachment, data);
      }
      if (invoice == null) {
        log.debug("Support attachment {} not linked to active invoice, skip trip update", attachment.getId());
        return;
      }
      String ocrText = extractOcrText(data);
      TripPlaces places = ocrText.contains("货拉拉") && data.has("ocr_raw")
        ? extractHuolalaTripPlacesFromOcrRaw(data.get("ocr_raw"))
        : extractTripPlaces(ocrText);
      if (isEmpty(places.startPlace()) && isEmpty(places.endPlace()) && data.has("ocr_raw")) {
        places = extractHuolalaTripPlacesFromOcrRaw(data.get("ocr_raw"));
      }
      boolean changed = false;
      if (!isEmpty(places.startPlace())) {
        invoice.setStartPlace(stripSupplementaryChars(places.startPlace()));
        changed = true;
      }
      if (!isEmpty(places.endPlace())) {
        invoice.setEndPlace(stripSupplementaryChars(places.endPlace()));
        changed = true;
      }
      if (changed || places.kilometers() != null) {
        invoice.setUpdatedBy(attachment.getUploadedBy());
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        updateInvoiceExpenseItemTrip(invoice, places, attachment.getUploadedBy());
        log.info("Support attachment {} updated trip places for invoice {}: {} -> {}",
          attachment.getId(), invoice.getId(), invoice.getStartPlace(), invoice.getEndPlace());
      }
    } catch (Exception e) {
      log.warn("Failed to update trip places from support OCR attachment {}: {}", attachment.getId(), e.getMessage(), e);
    }
  }

  private Invoice linkSupportAttachmentByInvoiceNo(Attachment attachment, JsonNode data) {
    if (attachment.getReimbursementId() == null) return null;
    String invoiceNo = getJsonField(data, "invoice_no");
    String ocrText = extractOcrText(data);
    if (isEmpty(invoiceNo) && !isEmpty(ocrText)) {
      invoiceNo = regexExtract(ocrText, "发票号码[：:\\s]*(\\d{8,20})");
      if (isEmpty(invoiceNo)) invoiceNo = regexExtract(ocrText, "(?<!\\d)(\\d{20})(?!\\d)");
    }
    if (isEmpty(invoiceNo)) return null;
    String matchedInvoiceNo = invoiceNo;
    List<Invoice> matches = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(attachment.getReimbursementId()).stream()
      .filter(invoice -> matchedInvoiceNo.equals(invoice.getInvoiceNo()))
      .toList();
    if (matches.size() != 1) return null;
    Invoice invoice = matches.get(0);
    Long existingSupportId = invoice.getSupportAttachmentId();
    if (existingSupportId != null && !existingSupportId.equals(attachment.getId())) return null;
    invoice.setSupportAttachmentId(attachment.getId());
    invoice.setUpdatedBy(attachment.getUploadedBy());
    invoice.setUpdatedAt(LocalDateTime.now());
    return invoiceRepository.save(invoice);
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

  private void applyInvoiceOcrFields(Invoice invoice, Attachment attachment, String ocrResultJson,
                                     String invoiceNo, String invoiceCode, LocalDateTime invoiceDate,
                                     String buyerName, String sellerName, String buyerTaxId, String sellerTaxId,
                                     String totalAmount, String invoiceType, String invoiceStatus,
                                     String amountBeforeTax, String taxRate, String taxAmount,
                                     String payee, String reviewer, String issuer,
                                     String buyerAddressPhone, String buyerBankInfo,
                                     String sellerAddressPhone, String sellerBankInfo, String remarks) {
    invoice.setImagePath(attachment.getFileUrl());
    invoice.setOcrRawJson(stripSupplementaryChars(ocrResultJson));
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
    var cnDate = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日").matcher(date);
    if (cnDate.find()) {
      return LocalDate.of(
        Integer.parseInt(cnDate.group(1)),
        Integer.parseInt(cnDate.group(2)),
        Integer.parseInt(cnDate.group(3))
      ).atStartOfDay();
    }
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

  private String extractDateFromText(String text) {
    if (isEmpty(text)) return "";
    var cnDate = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日)").matcher(text);
    if (cnDate.find()) return cnDate.group(1);
    var isoDate = Pattern.compile("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})").matcher(text);
    return isoDate.find() ? isoDate.group(1) : "";
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
    ReimbursementExpenseItem item = invoice.getExpenseItemId() == null ? null : expenseItemRepository.findById(invoice.getExpenseItemId()).orElse(null);
    if (item == null) item = expenseItemRepository.findByInvoiceId(invoice.getId()).orElse(null);
    if (item == null) {
      item = new ReimbursementExpenseItem();
      item.setReimbursementId(reimbursement.getId());
      item.setInvoiceId(invoice.getId());
      item.setCreatedBy(attachment.getUploadedBy());
      item.setCreatedAt(now);
      int nextOrder = expenseItemRepository
        .findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursement.getId()).size() + 1;
      item.setSortOrder(nextOrder);
    }
    item.setInvoiceId(invoice.getId());
    item.setExpenseDate(invoice.getInvoiceDate());
    item.setExpenseType(expenseType);
    item.setOaExpenseName(oaExpenseName(expenseType));
    item.setRemark(null);
    item.setDescription(mergeInvoiceRemarks(item.getDescription(), List.of(invoice)));
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

  public void regroupExpenseItemsByInvoiceTrips(Long reimbursementId, Long userId) {
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId);
    Map<ExpenseGroupKey, List<Invoice>> groups = new LinkedHashMap<>();
    for (Invoice invoice : invoices) {
      ExpenseGroupKey key = groupKey(invoice);
      if (key == null) continue;
      groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(invoice);
    }
    LocalDateTime now = LocalDateTime.now();
    java.util.Set<Long> groupedInvoiceIds = new java.util.HashSet<>();
    for (List<Invoice> group : groups.values()) {
      if (group.size() < 2) continue;
      ReimbursementExpenseItem target = findOrCreateTripGroupItem(reimbursementId, group, userId, now);
      BigDecimal total = BigDecimal.ZERO;
      BigDecimal kilometers = sumKilometers(group);
      for (Invoice invoice : group) {
        groupedInvoiceIds.add(invoice.getId());
        invoice.setExpenseItemId(target.getId());
        invoice.setUpdatedBy(userId);
        invoice.setUpdatedAt(now);
        invoiceRepository.save(invoice);
        if (invoice.getTotalAmount() != null) total = total.add(invoice.getTotalAmount());
      }
      Invoice first = group.get(0);
      target.setInvoiceId(null);
      target.setExpenseDate(first.getInvoiceDate());
      target.setExpenseType(classifyExpenseType(first, first.getOcrRawJson()));
      target.setOaExpenseName(oaExpenseName(target.getExpenseType()));
      target.setAmount(total);
      target.setKilometers(kilometers);
      target.setRemark(null);
      target.setDescription(mergeInvoiceRemarks(target.getDescription(), group));
      target.setSource("ocr");
      target.setUpdatedBy(userId);
      target.setUpdatedAt(now);
      expenseItemRepository.save(target);
    }
    for (Invoice invoice : invoices) {
      if (groupedInvoiceIds.contains(invoice.getId())) continue;
      ReimbursementExpenseItem item = findOrCreateSingleInvoiceItem(reimbursementId, invoice, userId, now);
      invoice.setExpenseItemId(item.getId());
      invoice.setUpdatedBy(userId);
      invoice.setUpdatedAt(now);
      invoiceRepository.save(invoice);
    }
    removeOrphanExpenseItems(reimbursementId);
  }

  private ExpenseGroupKey groupKey(Invoice invoice) {
    if (invoice.getInvoiceDate() == null) return null;
    String date = invoice.getInvoiceDate().toLocalDate().toString();
    String expenseType = classifyExpenseType(invoice, invoice.getOcrRawJson());
    if (!canGroupByDateAndType(expenseType)) return null;
    return new ExpenseGroupKey("type", date, expenseType, "");
  }

  private ReimbursementExpenseItem findOrCreateTripGroupItem(Long reimbursementId, List<Invoice> group, Long userId, LocalDateTime now) {
    return group.stream()
      .map(Invoice::getExpenseItemId)
      .filter(id -> id != null)
      .map(id -> expenseItemRepository.findById(id).orElse(null))
      .filter(item -> item != null)
      .findFirst()
      .orElseGet(() -> {
        ReimbursementExpenseItem item = new ReimbursementExpenseItem();
        item.setReimbursementId(reimbursementId);
        item.setCreatedBy(userId);
        item.setCreatedAt(now);
        item.setSortOrder(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursementId).size() + 1);
        return expenseItemRepository.save(item);
      });
  }

  private ReimbursementExpenseItem findOrCreateSingleInvoiceItem(Long reimbursementId, Invoice invoice, Long userId, LocalDateTime now) {
    ReimbursementExpenseItem item = invoice.getExpenseItemId() == null ? null : expenseItemRepository.findById(invoice.getExpenseItemId()).orElse(null);
    if (item == null) item = expenseItemRepository.findByInvoiceId(invoice.getId()).orElse(null);
    if (item == null || !reimbursementId.equals(item.getReimbursementId())) {
      item = new ReimbursementExpenseItem();
      item.setReimbursementId(reimbursementId);
      item.setInvoiceId(invoice.getId());
      item.setCreatedBy(userId);
      item.setCreatedAt(now);
      item.setSortOrder(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursementId).size() + 1);
    }
    item.setInvoiceId(invoice.getId());
    item.setExpenseDate(invoice.getInvoiceDate());
    item.setExpenseType(classifyExpenseType(invoice, invoice.getOcrRawJson()));
    item.setOaExpenseName(oaExpenseName(item.getExpenseType()));
    item.setAmount(invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount());
    item.setRemark(null);
    item.setDescription(mergeInvoiceRemarks(item.getDescription(), List.of(invoice)));
    item.setSource("ocr");
    item.setUpdatedBy(userId);
    item.setUpdatedAt(now);
    return expenseItemRepository.save(item);
  }

  private void removeOrphanExpenseItems(Long reimbursementId) {
    List<Long> boundIds = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId).stream()
      .map(Invoice::getExpenseItemId)
      .filter(id -> id != null)
      .toList();
    expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(reimbursementId).stream()
      .filter(item -> "ocr".equals(item.getSource()))
      .filter(item -> item.getId() != null && !boundIds.contains(item.getId()))
      .forEach(expenseItemRepository::delete);
  }

  private void syncExpenseItemDescriptionsFromInvoiceRemarks(Long reimbursementId, Long userId) {
    List<Invoice> invoices = invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId);
    Map<Long, List<Invoice>> byExpenseItem = invoices.stream()
      .filter(invoice -> invoice.getExpenseItemId() != null)
      .collect(java.util.stream.Collectors.groupingBy(Invoice::getExpenseItemId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
    LocalDateTime now = LocalDateTime.now();
    for (Map.Entry<Long, List<Invoice>> entry : byExpenseItem.entrySet()) {
      ReimbursementExpenseItem item = expenseItemRepository.findById(entry.getKey()).orElse(null);
      if (item == null) continue;
      String merged = mergeInvoiceRemarks(item.getDescription(), entry.getValue());
      if (isEmpty(merged) || merged.equals(item.getDescription())) continue;
      item.setDescription(merged);
      item.setUpdatedBy(userId);
      item.setUpdatedAt(now);
      expenseItemRepository.save(item);
    }
  }

  private void updateInvoiceExpenseItemTrip(Invoice invoice, TripPlaces places, Long userId) {
    if (invoice.getExpenseItemId() == null) return;
    ReimbursementExpenseItem item = expenseItemRepository.findById(invoice.getExpenseItemId()).orElse(null);
    if (item == null) return;
    if (!isEmpty(places.startPlace())) item.setStartPlace(stripSupplementaryChars(places.startPlace()));
    if (!isEmpty(places.endPlace())) item.setEndPlace(stripSupplementaryChars(places.endPlace()));
    if (places.kilometers() != null) item.setKilometers(places.kilometers());
    item.setUpdatedBy(userId);
    item.setUpdatedAt(LocalDateTime.now());
    expenseItemRepository.save(item);
  }

  private BigDecimal sumKilometers(List<Invoice> invoices) {
    BigDecimal total = BigDecimal.ZERO;
    boolean hasValue = false;
    for (Invoice invoice : invoices) {
      if (invoice.getExpenseItemId() == null) continue;
      ReimbursementExpenseItem item = expenseItemRepository.findById(invoice.getExpenseItemId()).orElse(null);
      if (item == null || item.getKilometers() == null) continue;
      total = total.add(item.getKilometers());
      hasValue = true;
    }
    return hasValue ? total : null;
  }

  private boolean canGroupByDateAndType(String expenseType) {
    return expenseType != null && !expenseType.isBlank() && !isTransportExpenseType(expenseType);
  }

  private boolean isTransportExpenseType(String expenseType) {
    return Set.of(
      "toll",
      "rail",
      "metro",
      "ride_hailing",
      "carpool",
      "freight",
      "air_travel",
      "taxi",
      "bus",
      "rental_car",
      "parking"
    ).contains(expenseType);
  }

  private record ExpenseGroupKey(String mode, String date, String first, String second) {}

  private String mergeInvoiceRemarks(String current, List<Invoice> invoices) {
    LinkedHashMap<String, String> lines = new LinkedHashMap<>();
    if (!isEmpty(current)) {
      for (String line : current.replace("\r", "\n").split("\\n+")) {
        String cleaned = cleanRemarks(line);
        if (!isEmpty(cleaned)) lines.put(cleaned, cleaned);
      }
    }
    for (Invoice invoice : invoices) {
      if (invoice == null || isEmpty(invoice.getRemarks())) continue;
      for (String line : invoice.getRemarks().replace("\r", "\n").split("\\n+")) {
        String cleaned = cleanRemarks(line);
        if (!isEmpty(cleaned)) lines.put(cleaned, cleaned);
      }
    }
    return lines.isEmpty() ? null : String.join("\n", lines.values());
  }

  private Invoice findDuplicateInvoice(Long reimbursementId, String invoiceNo, String invoiceCode,
                                       LocalDateTime invoiceDate, String totalAmount, Long attachmentId) {
    BigDecimal amount = toBigDecimal(totalAmount);
    return invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(reimbursementId).stream()
      .filter(invoice -> attachmentId == null || !attachmentId.equals(invoice.getAttachmentId()))
      .filter(invoice -> isSameInvoice(invoice, invoiceNo, invoiceCode, invoiceDate, amount))
      .findFirst()
      .orElse(null);
  }

  private boolean isSameInvoice(Invoice invoice, String invoiceNo, String invoiceCode,
                                LocalDateTime invoiceDate, BigDecimal amount) {
    if (!isEmpty(invoiceNo) && invoiceNo.equals(invoice.getInvoiceNo())) return true;
    if (!isEmpty(invoiceCode) && !isEmpty(invoiceNo)
      && invoiceCode.equals(invoice.getInvoiceCode()) && invoiceNo.equals(invoice.getInvoiceNo())) {
      return true;
    }
    if (isEmpty(invoiceNo) && !isEmpty(invoiceCode) && invoiceCode.equals(invoice.getInvoiceCode())) {
      return sameExpenseDate(invoice.getInvoiceDate(), invoiceDate) && sameMoney(invoice.getTotalAmount(), amount);
    }
    return false;
  }

  private boolean sameMoney(BigDecimal left, BigDecimal right) {
    if (left == null || right == null) return false;
    return left.subtract(right).abs().compareTo(new BigDecimal("0.01")) <= 0;
  }

  private InvoiceParties extractInvoiceParties(String ocrText) {
    if (isEmpty(ocrText)
      || !ocrText.contains("\u8d2d\u4e70\u65b9\u4fe1\u606f")
      || !ocrText.contains("\u9500\u552e\u65b9\u4fe1\u606f")) {
      return new InvoiceParties("", "", "", "");
    }
    List<String> names = regexExtractAll(ocrText, "\u540d\u79f0[\uff1a:]\\s*([^\\r\\n]+)");
    List<String> taxIds = regexExtractAll(ocrText, "\u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7[\uff1a:]\\s*([0-9A-Z]{12,30})");
    if (names.size() < 2) return new InvoiceParties("", "", "", "");
    return new InvoiceParties(
      cleanPartyValue(names.get(0)),
      cleanPartyValue(names.get(1)),
      taxIds.isEmpty() ? "" : taxIds.get(0),
      taxIds.size() < 2 ? "" : taxIds.get(1)
    );
  }

  private String cleanPartyValue(String value) {
    if (isEmpty(value)) return "";
    return value.trim().replaceAll("\\s+", "");
  }

  private boolean isLikelyIssuerName(String value) {
    if (isEmpty(value)) return false;
    String compact = value.trim().replaceAll("\\s+", "");
    if (compact.length() <= 1 || compact.length() > 8) return false;
    return !compact.matches(".*[\u5143\u5706\u6574\u58f9\u8d30\u53c1\u8086\u4f0d\u9646\u67d2\u634c\u7396\u62fe\u4f70\u4edf].*");
  }

  private record InvoiceParties(String buyerName, String sellerName, String buyerTaxId, String sellerTaxId) {}

  private TripPlaces extractTripPlaces(String ocrText) {
    if (isEmpty(ocrText)) return new TripPlaces("", "", null);
    TripPlaces ctripTrip = extractCtripTripPlaces(ocrText);
    if (!isEmpty(ctripTrip.startPlace()) || !isEmpty(ctripTrip.endPlace())) return ctripTrip;
    TripPlaces didiTrip = extractDidiTripTablePlaces(ocrText);
    if (!isEmpty(didiTrip.startPlace()) || !isEmpty(didiTrip.endPlace())) return didiTrip;
    String start = firstNonBlank(
      regexExtract(ocrText, "(?:出发地|始发地|始发)[：:\\s]*([^\\r\\n]{2,40})"),
      regexExtract(ocrText, "出发地\\s+到达地[\\s\\S]{0,80}\\n\\s*([^\\r\\n\\s]{2,40})\\s+([^\\r\\n\\s]{2,40})")
    );
    String end = firstNonBlank(
      regexExtract(ocrText, "(?:到达地|目的地)[：:\\s]*([^\\r\\n]{2,40})"),
      regexExtractGroup(ocrText, "出发地\\s+到达地[\\s\\S]{0,80}\\n\\s*([^\\r\\n\\s]{2,40})\\s+([^\\r\\n\\s]{2,40})", 2)
    );
    return new TripPlaces(cleanTripPlace(start), cleanTripPlace(end), extractKilometers(ocrText));
  }

  private TripPlaces extractHuolalaTripPlacesFromOcrRaw(JsonNode ocrRaw) {
    if (ocrRaw == null || !ocrRaw.isArray() || ocrRaw.isEmpty()) return new TripPlaces("", "", null);
    double maxX = 0;
    for (JsonNode item : ocrRaw) {
      JsonNode bbox = item.get("bbox");
      if (bbox == null || bbox.size() < 8) continue;
      for (int i = 0; i < bbox.size(); i += 2) {
        maxX = Math.max(maxX, bbox.get(i).asDouble());
      }
    }
    if (maxX <= 0) return new TripPlaces("", "", null);

    double addressCenter = 0;
    double kilometerCenter = 0;
    for (JsonNode item : ocrRaw) {
      JsonNode bbox = item.get("bbox");
      String text = item.has("text") ? item.get("text").asText() : "";
      if (bbox == null || bbox.size() < 8 || isEmpty(text)) continue;
      double x = averageBboxCoordinate(bbox, 0);
      if (text.contains("地址")) addressCenter = x;
      if (text.contains("里程")) kilometerCenter = x;
    }

    double firstAddressY = 0;
    for (JsonNode item : ocrRaw) {
      JsonNode bbox = item.get("bbox");
      String text = item.has("text") ? item.get("text").asText() : "";
      if (bbox == null || bbox.size() < 8 || isEmpty(text) || !text.trim().startsWith("●")) continue;
      double y = averageBboxCoordinate(bbox, 1);
      firstAddressY = firstAddressY == 0 ? y : Math.min(firstAddressY, y);
    }

    List<OcrRawText> addressTexts = new ArrayList<>();
    List<OcrRawText> kilometerTexts = new ArrayList<>();
    for (JsonNode item : ocrRaw) {
      JsonNode bbox = item.get("bbox");
      String text = item.has("text") ? item.get("text").asText() : "";
      if (bbox == null || bbox.size() < 8 || isEmpty(text)) continue;
      double x = averageBboxCoordinate(bbox, 0);
      double y = averageBboxCoordinate(bbox, 1);
      double ratio = x / maxX;
      boolean inAddressColumn = addressCenter > 0
        ? Math.abs(x - addressCenter) <= Math.max(70, maxX * 0.08)
        : ratio >= 0.46 && ratio <= 0.64;
      boolean inKilometerColumn = kilometerCenter > 0
        ? Math.abs(x - kilometerCenter) <= Math.max(45, maxX * 0.05)
        : ratio >= 0.67 && ratio <= 0.75;
      boolean afterAddressStart = firstAddressY == 0 || text.trim().startsWith("●") || y > firstAddressY + 5;
      if (inAddressColumn && afterAddressStart && !text.contains("地址")) {
        addressTexts.add(new OcrRawText(y, text));
      }
      if (inKilometerColumn && y > 360 && text.matches("\\d+(?:\\.\\d+)?")) {
        kilometerTexts.add(new OcrRawText(y, text));
      }
    }
    addressTexts.sort(java.util.Comparator.comparingDouble(OcrRawText::y));
    kilometerTexts.sort(java.util.Comparator.comparingDouble(OcrRawText::y));

    List<String> places = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (OcrRawText rawText : addressTexts) {
      String text = rawText.text().trim();
      if (text.startsWith("●")) {
        if (!current.isEmpty()) places.add(cleanTripPlace(current.toString()));
        current.setLength(0);
      }
      current.append(text);
    }
    if (!current.isEmpty()) places.add(cleanTripPlace(current.toString()));

    String start = places.isEmpty() ? "" : places.get(0);
    String end = places.size() < 2 ? "" : places.get(1);
    BigDecimal kilometers = kilometerTexts.isEmpty() ? null : toBigDecimal(kilometerTexts.get(0).text());
    return new TripPlaces(start, end, kilometers);
  }

  private double averageBboxCoordinate(JsonNode bbox, int offset) {
    double total = 0;
    int count = 0;
    for (int i = offset; i < bbox.size(); i += 2) {
      total += bbox.get(i).asDouble();
      count++;
    }
    return count == 0 ? 0 : total / count;
  }

  private record OcrRawText(double y, String text) {}

  private record BboxWord(String text, JsonNode bbox) {
    double cx() {
      return (min(true) + max(true)) / 2;
    }

    double cy() {
      return (min(false) + max(false)) / 2;
    }

    double maxX() {
      return max(true);
    }

    private double min(boolean xAxis) {
      double result = Double.MAX_VALUE;
      for (int i = xAxis ? 0 : 1; i < bbox.size(); i += 2) result = Math.min(result, bbox.get(i).asDouble());
      return result == Double.MAX_VALUE ? 0 : result;
    }

    private double max(boolean xAxis) {
      double result = -Double.MAX_VALUE;
      for (int i = xAxis ? 0 : 1; i < bbox.size(); i += 2) result = Math.max(result, bbox.get(i).asDouble());
      return result == -Double.MAX_VALUE ? 0 : result;
    }
  }

  private TripPlaces extractCtripTripPlaces(String ocrText) {
    String normalized = ocrText.replace("\r", "\n");
    String start = nextNonBlankLineAfter(normalized, "上车点：", "上车点:");
    String end = nextNonBlankLineAfter(normalized, "下车点：", "下车点:");
    if (isEmpty(start) && isEmpty(end)) return new TripPlaces("", "", null);
    return new TripPlaces(cleanTripPlace(start), cleanTripPlace(end), extractKilometers(ocrText));
  }

  private TripPlaces extractDidiTripTablePlaces(String ocrText) {
    String normalized = ocrText.replace("\r", "\n").replace('|', '\n');
    String[] lines = normalized.split("\\n+");
    TripPlaces qunarPlaces = extractQunarTripTablePlaces(lines);
    if (!isEmpty(qunarPlaces.startPlace()) || !isEmpty(qunarPlaces.endPlace())) return qunarPlaces;
    TripPlaces verticalPlaces = extractDidiVerticalTripTablePlaces(lines);
    if (!isEmpty(verticalPlaces.startPlace()) || !isEmpty(verticalPlaces.endPlace())) return verticalPlaces;
    for (String line : lines) {
      String compact = line.trim().replaceAll("\\s+", " ");
      if (!compact.matches("^\\d+\\s+.*\\d+(?:\\.\\d+)?\\s+\\d+(?:\\.\\d+)?(?:\\s+.*)?$")) continue;
      var m = Pattern.compile("^\\d+\\s+\\S+\\s+\\S+\\s+\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}\\s+\\S+\\s+\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}\\s+\\S+\\s+(.+?)\\s+(.+?)\\s+\\d+(?:\\.\\d+)?\\s+\\d+(?:\\.\\d+)?(?:\\s+.*)?$").matcher(compact);
      if (m.find()) {
        BigDecimal kilometers = firstNumberAfter(compact, cleanTripPlace(m.group(2)));
        return new TripPlaces(cleanTripPlace(m.group(1)), cleanTripPlace(m.group(2)), kilometers);
      }
    }
    return new TripPlaces("", "", extractKilometers(ocrText));
  }

  private TripPlaces extractQunarTripTablePlaces(String[] lines) {
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (!line.matches("\\d+")) continue;
      String previous = String.join("\n", java.util.Arrays.copyOfRange(lines, Math.max(0, i - 12), i));
      if (!previous.contains("起点") || !previous.contains("终点") || !previous.contains("金额")) continue;
      if (i + 5 >= lines.length) continue;
      String time = lines[i + 2].trim();
      String start = lines[i + 3].trim();
      String end = lines[i + 4].trim();
      String amount = lines[i + 5].trim();
      if (!time.matches(".*\\d{4}-\\d{2}-\\d{2}.*\\d{1,2}:\\d{2}.*")) continue;
      if (!amount.matches("\\d+(?:\\.\\d+)?")) continue;
      return new TripPlaces(cleanTripPlace(start), cleanTripPlace(end), null);
    }
    return new TripPlaces("", "", null);
  }

  private TripPlaces extractDidiVerticalTripTablePlaces(String[] lines) {
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (!line.matches("\\d+")) continue;
      if (i + 8 >= lines.length) continue;
      String carType = lines[i + 1].trim();
      String city = lines[i + 2].trim();
      String departTime = lines[i + 3].trim();
      String arriveTime = lines[i + 4].trim();
      String start = lines[i + 5].trim();
      String end = lines[i + 6].trim();
      String mileage = lines[i + 7].trim();
      String amount = lines[i + 8].trim();
      if (!isTripDataRow(carType, city, departTime, arriveTime, start, end, mileage, amount)) continue;
      return new TripPlaces(cleanTripPlace(start), cleanTripPlace(end), toBigDecimal(mileage));
    }
    return new TripPlaces("", "", null);
  }

  private boolean isTripDataRow(String carType, String city, String departTime, String arriveTime,
                                String start, String end, String mileage, String amount) {
    return !isEmpty(carType)
      && !isEmpty(city)
      && departTime.matches(".*\\d{2}-?\\d{2}.*\\d{1,2}:\\d{2}.*")
      && arriveTime.matches(".*\\d{2}-?\\d{2}.*\\d{1,2}:\\d{2}.*")
      && !isEmpty(cleanTripPlace(start))
      && !isEmpty(cleanTripPlace(end))
      && mileage.matches("\\d+(?:\\.\\d+)?")
      && amount.matches("\\d+(?:\\.\\d+)?");
  }

  private String cleanTripPlace(String value) {
    if (isEmpty(value)) return "";
    return value
      .replaceAll("^[●•·\\s]+", "")
      .replaceAll("^(起点|终点|出发地|到达地)$", "")
      .replaceAll("(出发地|到达地|始发地|目的地|起点|终点|里程\\[公里\\]|里程|金额\\[元\\]|备注|等级|交通工具类型|金额|税率|税额).*$", "")
      .replaceAll("[：:]+$", "")
      .trim();
  }

  private String regexExtractGroup(String text, String pattern, int group) {
    var m = Pattern.compile(pattern).matcher(text);
    return m.find() ? m.group(group) : "";
  }

  private String nextNonBlankLineAfter(String text, String... labels) {
    String[] lines = text.split("\\n+");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      for (String label : labels) {
        if (!line.equals(label)) continue;
        for (int j = i + 1; j < lines.length; j++) {
          String candidate = lines[j].trim();
          if (!candidate.isBlank()) return candidate;
        }
      }
    }
    return "";
  }

  private BigDecimal extractKilometers(String ocrText) {
    String kilometers = regexExtract(ocrText, "(?:\\u91cc\\u7a0b|里程)\\s*(?:\\[\\s*(?:\\u516c\\u91cc|公里)\\s*\\])?\\s*[：:]?\\s*(\\d+(?:\\.\\d+)?)");
    return toBigDecimal(kilometers);
  }

  private BigDecimal firstNumberAfter(String text, String marker) {
    if (isEmpty(text) || isEmpty(marker)) return null;
    int index = text.indexOf(marker);
    if (index < 0) return null;
    var m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(text.substring(index + marker.length()));
    return m.find() ? toBigDecimal(m.group(1)) : null;
  }

  private record TripPlaces(String startPlace, String endPlace, BigDecimal kilometers) {}

  private String classifyExpenseType(Invoice invoice, String ocrText) {
    String normalizedText = String.join(" ",
      firstNonBlank(invoice.getRemarks()),
      firstNonBlank(invoice.getSellerName()),
      firstNonBlank(invoice.getSellerShortName()),
      firstNonBlank(invoice.getInvoiceContentShortName()),
      firstNonBlank(ocrText)
    ).toLowerCase();
    for (ExpenseTypeRule rule : EXPENSE_TYPE_RULES) {
      if (containsAny(normalizedText, rule.keywords().toArray(String[]::new))) return rule.expenseType();
    }
    return "other";
  }

  private record ExpenseTypeRule(String expenseType, List<String> keywords) {}

  private boolean sameExpenseDate(LocalDateTime left, LocalDateTime right) {
    if (left == null || right == null) return left == null && right == null;
    return left.toLocalDate().equals(right.toLocalDate());
  }

  private boolean canAutoLinkToExpenseItem(LocalDateTime expenseDate, LocalDateTime invoiceDate) {
    return expenseDate == null || invoiceDate == null || sameExpenseDate(expenseDate, invoiceDate);
  }

  private String stripSupplementaryChars(String value) {
    if (value == null) return null;
    return value.codePoints()
      .filter(codePoint -> codePoint <= Character.MAX_VALUE)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString();
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
      case "fuel" -> "油费金额";
      case "toll" -> "高速费";
      case "rail" -> "高铁/动车/火车费";
      case "taxi" -> "出租车费";
      case "passenger_transport" -> "客运交通费";
      case "air_travel" -> "机票/航空费";
      case "hotel" -> "住宿费";
      case "insurance" -> "保险费";
      case "metro" -> "地铁费";
      case "ride_hailing" -> "网约车费";
      case "carpool" -> "顺风车费";
      case "freight" -> "货拉拉费";
      case "delivery" -> "快递/配送费";
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

  private List<String> regexExtractAll(String text, String pattern) {
    var m = Pattern.compile(pattern).matcher(text);
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    while (m.find()) {
      values.add(m.group(1));
    }
    return values;
  }

  private List<String> extractMoneyBefore(String text, String endMarker) {
    int end = text.indexOf(endMarker);
    String scope = end >= 0 ? text.substring(0, end) : text;
    var m = Pattern.compile("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)").matcher(scope);
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    while (m.find()) {
      values.add(m.group(1));
    }
    return values;
  }

  private List<String> extractSummaryAmountPair(String text) {
    java.util.ArrayList<String> values = new java.util.ArrayList<>();
    var m = Pattern.compile("合\\s*计[\\s\\S]{0,20}[￥¥]\\s*(\\d+(?:\\.\\d{1,2})?)[\\s\\S]{0,20}[￥¥]\\s*(\\d+(?:\\.\\d{1,2})?)").matcher(text);
    if (m.find()) {
      values.add(m.group(1));
      values.add(m.group(2));
    }
    return values;
  }

  String extractTotalAmountFromText(String text) {
    String amount = regexExtract(text, "[（(]\\s*小写\\s*[）)]\\s*[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)");
    if (!isEmpty(amount)) return amount;
    amount = regexExtract(text, "小写\\s*[）)]?\\s*[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)");
    if (!isEmpty(amount)) return amount;
    return regexExtract(text, "(?:价税合计|合计)[\\s\\S]{0,20}?[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)");
  }

  private boolean shouldUseSummaryAmountPair(String totalAmount, List<String> summaryAmounts,
                                             String amountBeforeTax, String taxAmount) {
    if (summaryAmounts.size() < 2) return false;
    BigDecimal total = toBigDecimal(totalAmount);
    BigDecimal summaryAmount = toBigDecimal(summaryAmounts.get(0));
    BigDecimal summaryTax = toBigDecimal(summaryAmounts.get(1));
    if (total == null || summaryAmount == null || summaryTax == null) return false;
    BigDecimal summaryTotal = summaryAmount.add(summaryTax);
    boolean summaryMatchesTotal = summaryTotal.subtract(total).abs().compareTo(new BigDecimal("0.02")) <= 0;
    if (!summaryMatchesTotal) return false;
    BigDecimal currentAmount = toBigDecimal(amountBeforeTax);
    BigDecimal currentTax = toBigDecimal(taxAmount);
    if (currentAmount == null || currentTax == null) return true;
    BigDecimal currentTotal = currentAmount.add(currentTax);
    return currentTotal.subtract(total).abs().compareTo(new BigDecimal("0.02")) > 0;
  }

  String reconcileTotalAmountFromSummary(String totalAmount, List<String> summaryAmounts) {
    if (summaryAmounts.size() < 2) return totalAmount;
    BigDecimal summaryAmount = toBigDecimal(summaryAmounts.get(0));
    BigDecimal summaryTax = toBigDecimal(summaryAmounts.get(1));
    if (summaryAmount == null || summaryTax == null) return totalAmount;
    BigDecimal summaryTotal = summaryAmount.add(summaryTax).setScale(2, java.math.RoundingMode.HALF_UP);
    if (isEmpty(totalAmount)) return summaryTotal.toPlainString();

    BigDecimal total = toBigDecimal(totalAmount);
    if (total == null) return totalAmount;
    if (summaryTotal.subtract(total).abs().compareTo(new BigDecimal("0.02")) <= 0) return totalAmount;

    String normalizedTotal = total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    String normalizedSummaryTotal = summaryTotal.toPlainString();
    if (summaryTotal.compareTo(total) > 0 && normalizedSummaryTotal.endsWith(normalizedTotal)) {
      return normalizedSummaryTotal;
    }
    return totalAmount;
  }

  private boolean isBetterInvoiceNo(String candidate, String current) {
    if (isEmpty(candidate)) return false;
    if (isEmpty(current)) return true;
    return candidate.length() > current.length() && candidate.startsWith(current);
  }

  private boolean isEmpty(String s) {
    return s == null || s.isBlank();
  }

  private String recognizeInvoiceFile(Path filePath, String filename) throws Exception {
    String ext = getExtension(filename);
    if ("xml".equals(ext)) {
      return buildStructuredInvoiceOcrResult("xml", Files.readString(filePath, StandardCharsets.UTF_8), filename);
    }
    if ("ofd".equals(ext)) {
      return recognizeOfdInvoice(filePath, filename);
    }
    if ("pdf".equals(ext)) {
      return recognizePdfInvoice(filePath, filename);
    }
    byte[] imageBytes = Files.readAllBytes(filePath);
    String base64 = Base64.getEncoder().encodeToString(imageBytes);
    return callOcrService(base64, filename);
  }

  private String recognizePdfInvoice(Path filePath, String filename) throws Exception {
    try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
      if (document.getNumberOfPages() == 0) {
        throw new IllegalArgumentException("PDF没有可识别页面");
      }
      String text = new PDFTextStripper().getText(document);
      Map<String, String> fields = extractInvoiceFieldsFromText(text, filename);
      boolean useBboxParser = ocrExecutionService != null && ocrExecutionService.useBboxInvoiceParser();
      if (!useBboxParser && !isEmpty(fields.get("total_amount"))) {
        return buildStructuredInvoiceOcrResult("pdf-text", text, fields);
      }
      if (ocrExecutionService != null && ocrExecutionService.isExternalProviderSelected()) {
        log.info("PDF {} has no structured text fields, render first page for managed OCR", filename);
      }
      BufferedImage firstPage = new PDFRenderer(document).renderImageWithDPI(0, 200);
      ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
      ImageIO.write(firstPage, "png", imageBytes);
      String ocrJson = callOcrService(Base64.getEncoder().encodeToString(imageBytes.toByteArray()), replaceExtension(filename, "png"));
      if (useBboxParser) {
        return enhanceInvoiceFieldsFromOcrRaw(ocrJson, filename, fields);
      }
      return ocrJson;
    }
  }

  private String recognizeOfdInvoice(Path filePath, String filename) throws Exception {
    Map<String, String> fields = new LinkedHashMap<>();
    List<String> textParts = new ArrayList<>();
    try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
      var entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }
        String xml = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
        mergeMissing(fields, extractInvoiceFieldsFromXml(xml, filename));
        String text = extractOfdTextCodes(xml);
        if (!isEmpty(text)) {
          textParts.add(text);
          mergeMissing(fields, extractInvoiceFieldsFromText(text, filename));
        }
      }
    }
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("OFD文件未解析到发票字段");
    }
    return buildStructuredInvoiceOcrResult("ofd", String.join("\n", textParts), fields);
  }

  private String buildStructuredInvoiceOcrResult(String fileType, String xmlText, String filename) throws Exception {
    Map<String, String> fields = extractInvoiceFieldsFromXml(xmlText, filename);
    if (fields.isEmpty()) {
      fields = extractInvoiceFieldsFromText(xmlText, filename);
    }
    if (fields.isEmpty()) {
      throw new IllegalArgumentException(fileType.toUpperCase() + "文件未解析到发票字段");
    }
    return buildStructuredInvoiceOcrResult(fileType, xmlText, fields);
  }

  private String buildStructuredInvoiceOcrResult(String fileType, String ocrText, Map<String, String> fields) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("status", "ok");
    root.put("source_type", fileType);
    root.put("ocr_text", ocrText == null ? "" : ocrText);
    ObjectNode invoiceFields = objectMapper.createObjectNode();
    fields.forEach((key, value) -> {
      if (!isEmpty(value)) invoiceFields.put(key, value);
    });
    root.set("invoice_fields", invoiceFields);
    return objectMapper.writeValueAsString(root);
  }

  String enhanceInvoiceFieldsFromOcrRaw(String ocrJson, String filename) throws Exception {
    return enhanceInvoiceFieldsFromOcrRaw(ocrJson, filename, Map.of());
  }

  private String enhanceInvoiceFieldsFromOcrRaw(String ocrJson, String filename, Map<String, String> textFields) throws Exception {
    JsonNode root = objectMapper.readTree(ocrJson);
    Map<String, String> bboxFields = new LinkedHashMap<>(extractInvoiceFieldsFromOcrRaw(root, filename));
    if (!textFields.isEmpty()) {
      mergeMissing(bboxFields, textFields);
      if (!isEmpty(textFields.get("issuer"))) {
        bboxFields.put("issuer", textFields.get("issuer"));
      }
    }
    if (bboxFields.isEmpty()) return ocrJson;
    ObjectNode mutable = root.deepCopy();
    ObjectNode invoiceFields = mutable.withObject("/invoice_fields");
    bboxFields.forEach((key, value) -> {
      if (!isEmpty(value)) invoiceFields.put(key, value);
    });
    mutable.put("parse_strategy", "BBOX_REGION");
    return objectMapper.writeValueAsString(mutable);
  }

  Map<String, String> extractInvoiceFieldsFromOcrRaw(JsonNode root, String filename) {
    JsonNode raw = root.has("ocr_raw") ? root.path("ocr_raw") : root.path("raw").path("ocr_raw");
    List<BboxWord> boxes = new ArrayList<>();
    for (JsonNode item : raw) {
      addInvoiceBboxWord(boxes, item);
    }
    if (boxes.isEmpty()) {
      for (JsonNode page : root.path("pages")) {
        for (JsonNode item : page.path("lines")) {
          addInvoiceBboxWord(boxes, item);
        }
      }
    }
    if (boxes.isEmpty()) return Map.of();
    boxes.sort(Comparator.comparingDouble(BboxWord::cy).thenComparingDouble(BboxWord::cx));

    String text = boxes.stream().map(BboxWord::text).reduce("", (a, b) -> a + "\n" + b);
    Map<String, String> fields = new LinkedHashMap<>();
    putIfNotEmpty(fields, "invoice_no", regexExtract(text, "(\\d{20})"));
    putIfNotEmpty(fields, "date", regexExtract(text, "(\\d{4}年\\d{1,2}月\\d{1,2}日)"));
    fillPartyFieldsFromBbox(fields, boxes);
    fillAmountFieldsFromBbox(fields, boxes);
    putIfNotEmpty(fields, "issuer", extractIssuerFromBbox(boxes));
    putIfNotEmpty(fields, "remarks", extractRemarksFromBbox(boxes));
    normalizeStructuredFields(fields, filename);
    return fields;
  }

  private void addInvoiceBboxWord(List<BboxWord> boxes, JsonNode item) {
    JsonNode bbox = item.path("bbox");
    if (!bbox.isArray()) return;
    var coordinates = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    for (JsonNode coordinate : bbox) {
      if (coordinate.isArray()) {
        if (coordinate.size() != 2 || !coordinate.get(0).isNumber() || !coordinate.get(1).isNumber()) return;
        coordinates.add(coordinate.get(0));
        coordinates.add(coordinate.get(1));
      } else {
        if (!coordinate.isNumber()) return;
        coordinates.add(coordinate);
      }
    }
    if (coordinates.size() == 8) {
      boxes.add(new BboxWord(item.path("text").asText(""), coordinates));
    }
  }

  private void fillPartyFieldsFromBbox(Map<String, String> fields, List<BboxWord> boxes) {
    double splitX = maxX(boxes) / 2;
    List<String> taxIds = boxes.stream()
      .map(BboxWord::text)
      .map(text -> regexExtract(text, "([0-9A-Z]{15,20})"))
      .filter(value -> !isEmpty(value))
      .distinct()
      .toList();
    if (taxIds.size() >= 2) {
      putIfNotEmpty(fields, "buyer_tax_id", taxIds.get(0));
      putIfNotEmpty(fields, "seller_tax_id", taxIds.get(1));
    }
    putIfNotEmpty(fields, "buyer_name", extractNameNearLabel(boxes, b -> b.cx() < splitX));
    putIfNotEmpty(fields, "seller_name", extractNameNearLabel(boxes, b -> b.cx() >= splitX));
  }

  private String extractNameNearLabel(List<BboxWord> boxes, java.util.function.Predicate<BboxWord> side) {
    return boxes.stream()
      .filter(side)
      .filter(box -> box.text().contains("名称"))
      .map(box -> {
        String value = afterColon(box.text());
        if (!isEmpty(value)) return value;
        return nearestText(boxes, box, candidate -> candidate.cx() > box.cx() && Math.abs(candidate.cy() - box.cy()) < 35, 260);
      })
      .filter(value -> !isEmpty(value) && !value.contains("信息"))
      .findFirst()
      .orElse("");
  }

  private void fillAmountFieldsFromBbox(Map<String, String> fields, List<BboxWord> boxes) {
    List<BboxWord> summaryAmounts = moneyBoxesNearAnchor(boxes, "合计", 55);
    if (summaryAmounts.size() >= 2) {
      putIfNotEmpty(fields, "amount_before_tax", money(summaryAmounts.get(0).text()));
      putIfNotEmpty(fields, "tax_amount", money(summaryAmounts.get(summaryAmounts.size() - 1).text()));
    }
    if (isEmpty(fields.get("amount_before_tax")) || isEmpty(fields.get("tax_amount"))) {
      List<BboxWord> lineAmounts = tableLineAmountBoxes(boxes);
      if (lineAmounts.size() >= 2) {
        putIfNotEmpty(fields, "amount_before_tax", money(lineAmounts.get(lineAmounts.size() - 2).text()));
        putIfNotEmpty(fields, "tax_amount", money(lineAmounts.get(lineAmounts.size() - 1).text()));
      }
    }
    putIfNotEmpty(fields, "total_amount", extractTotalAmountFromBbox(boxes));
    boolean exempt = boxes.stream().anyMatch(box -> "免税".equals(box.text().trim()));
    Optional<BboxWord> taxHeader = boxes.stream().filter(box -> "税额".equals(box.text().trim())).findFirst();
    if (exempt && taxHeader.isPresent() && boxes.stream().anyMatch(box ->
        box.text().trim().matches("\\*{2,}") && box.cy() > taxHeader.get().cy()
          && Math.abs(box.cx() - taxHeader.get().cx()) < 80)) {
      fields.put("tax_amount", "0.00");
      fields.put("tax_rate", "免税");
      if (isEmpty(fields.get("amount_before_tax"))) {
        putIfNotEmpty(fields, "amount_before_tax", fields.get("total_amount"));
      }
    }
  }

  private List<BboxWord> moneyBoxesNearAnchor(List<BboxWord> boxes, String anchorText, double yWindow) {
    Optional<BboxWord> anchor = boxes.stream()
      .filter(box -> box.text().contains(anchorText) || ("合计".equals(anchorText) && ("合".equals(box.text()) || "计".equals(box.text()))))
      .filter(box -> !box.text().contains("价税"))
      .findFirst();
    if (anchor.isEmpty()) return List.of();
    double y = anchor.get().cy();
    return boxes.stream()
      .filter(box -> box.cx() > anchor.get().cx())
      .filter(box -> box.cy() >= y - 45 && box.cy() <= y + yWindow)
      .filter(box -> !isEmpty(money(box.text())))
      .sorted(Comparator.comparingDouble(BboxWord::cx))
      .toList();
  }

  private List<BboxWord> tableLineAmountBoxes(List<BboxWord> boxes) {
    double headerY = boxes.stream()
      .filter(box -> box.text().contains("项目名称"))
      .mapToDouble(box -> box.cy() - 35)
      .findFirst()
      .orElse(0);
    if (headerY == 0) return List.of();
    double bottomY = boxes.stream()
      .filter(box -> box.text().contains("合计") || "计".equals(box.text()))
      .mapToDouble(box -> box.cy() - 35)
      .filter(y -> y > headerY)
      .findFirst()
      .orElse(headerY + 260);
    return boxes.stream()
      .filter(box -> box.cy() > headerY && box.cy() < bottomY)
      .filter(box -> !isEmpty(money(box.text())))
      .sorted(Comparator.comparingDouble(BboxWord::cy).thenComparingDouble(BboxWord::cx))
      .toList();
  }

  private String extractTotalAmountFromBbox(List<BboxWord> boxes) {
    Optional<BboxWord> withSmall = boxes.stream()
      .filter(box -> box.text().contains("小写"))
      .findFirst();
    if (withSmall.isPresent()) {
      String own = money(withSmall.get().text());
      if (!isEmpty(own)) return own;
      String near = nearestText(boxes, withSmall.get(), box -> box.cx() > withSmall.get().cx() - 20 && Math.abs(box.cy() - withSmall.get().cy()) < 60, 360);
      return money(near);
    }
    List<BboxWord> totalAmounts = moneyBoxesNearAnchor(boxes, "价税合计", 120);
    return totalAmounts.isEmpty() ? "" : money(totalAmounts.get(totalAmounts.size() - 1).text());
  }

  private String extractIssuerFromBbox(List<BboxWord> boxes) {
    Optional<BboxWord> label = boxes.stream()
      .filter(box -> box.text().contains("开票人"))
      .reduce((first, second) -> second);
    if (label.isEmpty()) return "";
    String own = cleanPersonName(afterColon(label.get().text()));
    if (!isEmpty(own)) return own;
    String near = nearestText(boxes, label.get(),
      box -> Math.abs(box.cy() - label.get().cy()) < 55 || (box.cy() > label.get().cy() && box.cy() - label.get().cy() < 90),
      320);
    return cleanPersonName(near);
  }

  private String extractRemarksFromBbox(List<BboxWord> boxes) {
    Optional<BboxWord> remarksLabel = boxes.stream()
      .filter(box -> box.text().contains("备注"))
      .min(Comparator.comparingDouble(BboxWord::cy));
    if (remarksLabel.isEmpty()) return extractVerticalRemarksFromBbox(boxes);
    double labelY = remarksLabel.get().cy();
    double lowerBound = boxes.stream()
      .filter(box -> box.cy() > labelY)
      .filter(box -> box.text().contains("价税合计") || box.text().contains("开票人"))
      .mapToDouble(box -> box.cy() - 35)
      .min()
      .orElse(labelY + 160);
    if (lowerBound >= labelY + 155) {
      lowerBound = boxes.stream()
        .filter(box -> box.cy() > labelY)
        .filter(box -> box.cx() < 420)
        .filter(box -> box.text().contains(":") || box.text().contains("\uff1a"))
        .mapToDouble(box -> box.cy() - 35)
        .min()
        .orElse(lowerBound);
    }
    double remarksLowerBound = lowerBound;
    String value = boxes.stream()
      .filter(box -> box.cy() >= labelY - 25 && box.cy() < remarksLowerBound)
      .filter(box -> box.cx() > remarksLabel.get().cx() - 20)
      .sorted(Comparator.comparingDouble(BboxWord::cy).thenComparingDouble(BboxWord::cx))
      .map(BboxWord::text)
      .filter(text -> !isEmpty(text))
      .filter(text -> !text.replaceAll("\\s+", "").equals("备注"))
      .filter(text -> !text.contains("价税合计"))
      .filter(text -> !text.contains("开票人"))
      .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
    value = cleanRemarks(value);
    return isEmpty(value) ? extractVerticalRemarksFromBbox(boxes) : value;
  }

  private String extractVerticalRemarksFromBbox(List<BboxWord> boxes) {
    Optional<BboxWord> bei = boxes.stream()
      .filter(box -> "\u5907".equals(box.text().trim()))
      .filter(box -> box.cx() < 120)
      .min(Comparator.comparingDouble(BboxWord::cy));
    if (bei.isEmpty()) return "";
    Optional<BboxWord> zhu = boxes.stream()
      .filter(box -> "\u6ce8".equals(box.text().trim()))
      .filter(box -> Math.abs(box.cx() - bei.get().cx()) < 80)
      .filter(box -> box.cy() > bei.get().cy())
      .min(Comparator.comparingDouble(BboxWord::cy));
    if (zhu.isEmpty()) return "";
    double top = boxes.stream()
      .filter(box -> box.text().contains("\u5c0f\u5199"))
      .mapToDouble(BboxWord::cy)
      .filter(y -> y < bei.get().cy())
      .max()
      .orElse(bei.get().cy() - 80);
    double bottom = zhu.get().cy() + 25;
    List<BboxWord> remarkBoxes = boxes.stream()
      .filter(box -> box.cx() > bei.get().cx() + 35)
      .filter(box -> box.cy() > top)
      .filter(box -> box.cy() < bottom)
      .filter(box -> !box.text().contains("\u5c0f\u5199"))
      .filter(box -> !box.text().contains("\u5f00\u7968\u4eba"))
      .sorted(Comparator.comparingDouble(BboxWord::cy).thenComparingDouble(BboxWord::cx))
      .toList();
    return cleanRemarks(joinBboxLines(remarkBoxes));
  }

  private String joinBboxLines(List<BboxWord> boxes) {
    StringBuilder result = new StringBuilder();
    double currentY = Double.NaN;
    StringBuilder line = new StringBuilder();
    for (BboxWord box : boxes) {
      if (isEmpty(box.text())) continue;
      if (Double.isNaN(currentY) || Math.abs(box.cy() - currentY) <= 22) {
        if (line.length() > 0) line.append(" ");
        line.append(box.text().trim());
        currentY = Double.isNaN(currentY) ? box.cy() : (currentY + box.cy()) / 2;
      } else {
        if (result.length() > 0) result.append("\n");
        result.append(line);
        line.setLength(0);
        line.append(box.text().trim());
        currentY = box.cy();
      }
    }
    if (line.length() > 0) {
      if (result.length() > 0) result.append("\n");
      result.append(line);
    }
    return result.toString();
  }


  private String cleanRemarks(String text) {
    if (isEmpty(text)) return "";
    String value = text.replace("\r", "\n")
      .replaceAll("(?m)^\\s*备注[：:]?\\s*", "")
      .replaceAll("[ \\t]+", " ")
      .replaceAll("\\n{3,}", "\n\n")
      .trim();
    if (value.length() > 500) value = value.substring(0, 500).trim();
    return value;
  }

  private String extractRemarksFromText(String text) {
    if (isEmpty(text) || (!text.contains("\u5907\u6ce8") && !text.contains("\u5907\n\u6ce8"))) return "";
    String normalized = text.replace("\r", "\n");
    Matcher matcher = Pattern.compile("(?:\\u5907\\s*\\u6ce8)[\\uff1a:]?\\s*([\\s\\S]{0,500}?)(?:\\u4ef7\\u7a0e\\u5408\\u8ba1|\\u5f00\\u7968\\u4eba|$)")
      .matcher(normalized);
    String value = matcher.find() ? matcher.group(1) : "";
    if (isEmpty(value)) value = extractRemarksBeforeVerticalLabel(normalized);
    value = value.replaceAll("(?m)^\\s*(\\u5408\\u8ba1|\\u9879\\u76ee\\u540d\\u79f0|\\u89c4\\u683c\\u578b\\u53f7|\\u5355\\u4f4d|\\u6570\\u91cf|\\u5355\\u4ef7|\\u91d1\\u989d|\\u7a0e\\u7387|\\u7a0e\\u989d)\\s*$", "");
    return cleanRemarks(value);
  }

  private String extractRemarksBeforeVerticalLabel(String text) {
    Matcher label = Pattern.compile("(?m)^\\s*\\u5907\\s*\\n\\s*\\u6ce8\\s*$").matcher(text);
    if (!label.find()) return "";
    String beforeLabel = text.substring(0, label.start()).trim();
    Matcher afterTotal = Pattern.compile("(?:\\u5c0f\\u5199[\\uff09)]?\\s*[\\u00a5]?\\d+(?:\\.\\d{1,2})?)\\s*([\\s\\S]{0,500})$")
      .matcher(beforeLabel);
    String value = afterTotal.find() ? afterTotal.group(1) : lastNonBlankLine(beforeLabel);
    return value == null ? "" : value.trim();
  }

  private String lastNonBlankLine(String text) {
    if (isEmpty(text)) return "";
    String[] lines = text.split("\\n");
    for (int i = lines.length - 1; i >= 0; i--) {
      String line = lines[i].trim();
      if (!line.isEmpty()) return line;
    }
    return "";
  }

  private String nearestText(List<BboxWord> boxes, BboxWord anchor, java.util.function.Predicate<BboxWord> predicate, double maxDistance) {
    return boxes.stream()
      .filter(box -> box != anchor)
      .filter(predicate)
      .filter(box -> !isEmpty(box.text()))
      .min(Comparator.comparingDouble(box -> Math.abs(box.cy() - anchor.cy()) + Math.abs(box.cx() - anchor.cx()) / 3))
      .filter(box -> Math.abs(box.cy() - anchor.cy()) + Math.abs(box.cx() - anchor.cx()) / 3 <= maxDistance)
      .map(BboxWord::text)
      .orElse("");
  }

  private String afterColon(String text) {
    if (text == null) return "";
    int colon = Math.max(text.indexOf('：'), text.indexOf(':'));
    if (colon >= 0 && colon + 1 < text.length()) return text.substring(colon + 1).trim();
    int comma = Math.max(text.indexOf('，'), text.indexOf(','));
    if (comma >= 0 && comma + 1 < text.length()) return text.substring(comma + 1).trim();
    return "";
  }

  private String money(String text) {
    return regexExtract(text, "[¥￥]?\\s*(\\d+\\.\\d{1,2})");
  }

  private String cleanPersonName(String text) {
    if (isEmpty(text)) return "";
    String value = text.replace("开票人", "").replace("：", "").replace(":", "").replace("，", "").replace(",", "").trim();
    if (value.length() > 8 || value.matches(".*\\d.*") || value.contains("备注")) return "";
    return value;
  }

  private double maxX(List<BboxWord> boxes) {
    return boxes.stream().mapToDouble(BboxWord::maxX).max().orElse(0);
  }

  private Map<String, String> extractInvoiceFieldsFromXml(String xmlText, String filename) {
    Map<String, String> values = new LinkedHashMap<>();
    if (isEmpty(xmlText)) return values;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
      NodeList nodes = doc.getElementsByTagName("*");
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item(i);
        String key = normalizeXmlKey(element.getNodeName());
        String value = element.getTextContent() == null ? "" : element.getTextContent().trim();
        if (!isEmpty(key) && !isEmpty(value) && !value.contains("<") && value.length() <= 200) {
          values.putIfAbsent(key, value);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse invoice XML text, fallback to text extraction: {}", e.getMessage());
    }

    Map<String, String> fields = new LinkedHashMap<>();
    putFirst(fields, "invoice_no", values, "invoicenumber", "eiid", "invoiceno", "fphm");
    putFirst(fields, "invoice_code", values, "invoicecode", "fpdm");
    putFirst(fields, "date", values, "issuetime", "requesttime", "invoicedate", "kprq");
    putFirst(fields, "seller_name", values, "sellername", "xsfmc");
    putFirst(fields, "seller_tax_id", values, "selleridnum", "sellertaxno", "xsfnsrsbh");
    putFirst(fields, "buyer_name", values, "buyername", "gmfmc");
    putFirst(fields, "buyer_tax_id", values, "buyeridnum", "buyertaxno", "gmfnsrsbh");
    putFirst(fields, "total_amount", values, "totaltaxincludedamount", "totaltaxincludedamountinchinese", "jshj");
    putFirst(fields, "amount_before_tax", values, "totalamwithouttax", "amount");
    putFirst(fields, "tax_amount", values, "totaltaxam", "comtaxam");
    putFirst(fields, "tax_rate", values, "taxrate");
    putFirst(fields, "issuer", values, "drawer", "issuer");
    putFirst(fields, "invoice_type", values, "labelname", "invoicetype");

    normalizeStructuredFields(fields, filename);
    return fields;
  }

  private Map<String, String> extractInvoiceFieldsFromText(String text, String filename) {
    Map<String, String> fields = new LinkedHashMap<>();
    if (!isEmpty(text)) {
      putIfNotEmpty(fields, "invoice_no", regexExtract(text, "发票号码[:：]?\\s*(\\d{8,30})"));
      putIfNotEmpty(fields, "invoice_no", regexExtract(text, "(?<!\\d)(\\d{20})(?!\\d)"));
      putIfNotEmpty(fields, "date", regexExtract(text, "(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})"));
      if (text.contains("\u7535\u5b50\u53d1\u7968\uff08\u94c1\u8def\u7535\u5b50\u5ba2\u7968\uff09")) {
        fields.put("invoice_type", "\u7535\u5b50\u53d1\u7968\uff08\u94c1\u8def\u7535\u5b50\u5ba2\u7968\uff09");
      } else if (text.contains("\u822a\u7a7a\u8fd0\u8f93\u7535\u5b50\u5ba2\u7968\u884c\u7a0b\u5355")) {
        fields.put("invoice_type", "\u7535\u5b50\u53d1\u7968\uff08\u822a\u7a7a\u8fd0\u8f93\u7535\u5b50\u5ba2\u7968\u884c\u7a0b\u5355\uff09");
      }
      Matcher partyMatcher = Pattern.compile("开票人:\\s*([0-9A-Z]{18})([0-9A-Z]{18})(.+?有限公司)(.+?有限公司)(\\d{8,30})").matcher(text);
      if (partyMatcher.find()) {
        putIfNotEmpty(fields, "buyer_tax_id", partyMatcher.group(1));
        putIfNotEmpty(fields, "seller_tax_id", partyMatcher.group(2));
        putIfNotEmpty(fields, "buyer_name", partyMatcher.group(3));
        putIfNotEmpty(fields, "seller_name", partyMatcher.group(4));
        putIfNotEmpty(fields, "invoice_no", partyMatcher.group(5));
      }
      Matcher amountMatcher = Pattern.compile("[¥￥]\\s*(\\d+\\.\\d{2})[\\s\\S]{0,40}?[¥￥]\\s*(\\d+\\.\\d{2})[\\s\\S]{0,10}?[¥￥]\\s*(\\d+\\.\\d{2})").matcher(text);
      if (amountMatcher.find()) {
        putIfNotEmpty(fields, "amount_before_tax", amountMatcher.group(1));
        putIfNotEmpty(fields, "total_amount", amountMatcher.group(2));
        putIfNotEmpty(fields, "tax_amount", amountMatcher.group(3));
      }
      List<String> prefixedCurrencyAmounts = regexExtractAll(text, "[\u00a5\uffe5]\\s*(\\d+\\.\\d{2})");
      if (prefixedCurrencyAmounts.size() >= 3) {
        BigDecimal amountValue = toBigDecimal(prefixedCurrencyAmounts.get(0));
        BigDecimal taxValue = toBigDecimal(prefixedCurrencyAmounts.get(1));
        BigDecimal totalValue = toBigDecimal(prefixedCurrencyAmounts.get(2));
        if (amountValue != null && taxValue != null && totalValue != null
          && sameMoney(amountValue.add(taxValue), totalValue)) {
          fields.put("amount_before_tax", prefixedCurrencyAmounts.get(0));
          fields.put("tax_amount", prefixedCurrencyAmounts.get(1));
          fields.put("total_amount", prefixedCurrencyAmounts.get(2));
        } else if (amountValue != null && taxValue != null && totalValue != null
          && sameMoney(taxValue.add(totalValue), amountValue)) {
          fields.put("total_amount", prefixedCurrencyAmounts.get(0));
          fields.put("amount_before_tax", prefixedCurrencyAmounts.get(1));
          fields.put("tax_amount", prefixedCurrencyAmounts.get(2));
        }
      }
      String amountBeforeTax = fields.get("amount_before_tax");
      if (!isEmpty(amountBeforeTax)) {
        putIfNotEmpty(fields, "tax_rate", regexExtract(text, Pattern.quote(amountBeforeTax) + "(\\d+(?:\\.\\d+)?%)"));
      }
      putIfNotEmpty(fields, "issuer", regexExtract(text, "[¥￥]\\s*\\d+\\.\\d{2}([^\\d¥￥\\s]{1,20})$"));
      if (isEmpty(fields.get("issuer"))) {
        int issuerLabelIndex = text.indexOf("\u5f00\u7968\u4eba");
        if (issuerLabelIndex >= 0) {
          int issuerValueIndex = issuerLabelIndex;
          String totalAmount = fields.get("total_amount");
          if (!isEmpty(totalAmount)) {
            int totalAmountIndex = text.indexOf(totalAmount, issuerLabelIndex);
            if (totalAmountIndex >= 0) issuerValueIndex = totalAmountIndex + totalAmount.length();
          }
          String issuerText = text.substring(issuerValueIndex).replaceFirst("^.*?[\uff1a:]", "");
          List<String> issuerCandidates = regexExtractAll(issuerText,
            "(?m)^\\s*([^\\r\\n\\d\u00a5\uffe5?]{1,20})\\s*$");
          if (!issuerCandidates.isEmpty()) {
            fields.put("issuer", issuerCandidates.get(0).trim());
          }
        }
      }
      if (isEmpty(fields.get("total_amount"))) {
        putIfNotEmpty(fields, "total_amount", regexExtract(text, "价税合计[\\s\\S]{0,80}[¥￥]?\\s*(\\d+\\.\\d{2})"));
      }
      if (isEmpty(fields.get("total_amount"))) {
        putIfNotEmpty(fields, "total_amount", regexExtract(text,
          "(?:\\u7968\\u4ef7|\\u5e94\\u4ed8\\u91d1\\u989d|\\u5b9e\\u6536\\u91d1\\u989d)[:：\\s]*[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)"));
      }
      String summaryTotal = regexExtract(text,
        "\\u4ef7\\u7a0e\\u5408\\u8ba1[\\s\\S]{0,120}?(\\d+\\.\\d{2})");
      int summaryLabelIndex = text.indexOf("\u4ef7\u7a0e\u5408\u8ba1");
      if (summaryLabelIndex >= 0) {
        Matcher trailingCurrencySummary = Pattern.compile(
          "(\\d+\\.\\d{2})[\u00a5\uffe5?]\\s*(\\d+\\.\\d{2})[\u00a5\uffe5?][\\s\\S]{0,80}?[\u00a5\uffe5?]\\s*(\\d+\\.\\d{2})")
          .matcher(text.substring(summaryLabelIndex));
        if (trailingCurrencySummary.find()) {
          fields.put("amount_before_tax", trailingCurrencySummary.group(1));
          fields.put("tax_amount", trailingCurrencySummary.group(2));
          fields.put("total_amount", trailingCurrencySummary.group(3));
          summaryTotal = "";
        }
        List<String> currencySummaryAmounts = regexExtractAll(text.substring(summaryLabelIndex),
          "[\u00a5\uffe5]\\s*(\\d+\\.\\d{2})");
        if (!isEmpty(summaryTotal) && currencySummaryAmounts.size() >= 3) {
          BigDecimal firstAmount = toBigDecimal(currencySummaryAmounts.get(0));
          BigDecimal secondAmount = toBigDecimal(currencySummaryAmounts.get(1));
          BigDecimal thirdAmount = toBigDecimal(currencySummaryAmounts.get(2));
          if (sameMoney(firstAmount.add(secondAmount), thirdAmount)) {
            fields.put("amount_before_tax", currencySummaryAmounts.get(0));
            fields.put("tax_amount", currencySummaryAmounts.get(1));
            fields.put("total_amount", currencySummaryAmounts.get(2));
            summaryTotal = "";
          } else if (sameMoney(secondAmount.add(thirdAmount), firstAmount)) {
            fields.put("total_amount", currencySummaryAmounts.get(0));
            fields.put("amount_before_tax", currencySummaryAmounts.get(1));
            fields.put("tax_amount", currencySummaryAmounts.get(2));
            summaryTotal = "";
          }
        }
      }
      if (!isEmpty(summaryTotal)) {
        BigDecimal currentTotal = toBigDecimal(fields.get("total_amount"));
        BigDecimal summaryTotalValue = toBigDecimal(summaryTotal);
        if (summaryTotalValue != null && !sameMoney(currentTotal, summaryTotalValue)) {
          fields.put("total_amount", summaryTotal);
          int summaryTotalIndex = text.indexOf(summaryTotal, text.indexOf("\\u4ef7\\u7a0e\\u5408\\u8ba1"));
          if (summaryTotalIndex >= 0) {
            List<String> summaryAmounts = regexExtractAll(text.substring(summaryTotalIndex + summaryTotal.length()),
              "(\\d+\\.\\d{2})");
            if (summaryAmounts.size() >= 2) {
              fields.put("amount_before_tax", summaryAmounts.get(0));
              fields.put("tax_amount", summaryAmounts.get(1));
            }
          }
        }
      }
      String condensedText = text.replaceAll("\\s+", "");
      if (condensedText.contains("\u8d2d\u4e70\u65b9\u4fe1\u606f")
        && condensedText.contains("\u9500\u552e\u65b9\u4fe1\u606f")) {
        putIfNotEmpty(fields, "invoice_no", regexExtract(text, "(?<!\\d)(\\d{20})(?!\\d)"));
        List<String> taxIds = regexExtractAll(text, "(?<![0-9A-Z])([0-9A-Z]{18})(?![0-9A-Z])");
        if (taxIds.size() >= 2) {
          putIfNotEmpty(fields, "buyer_tax_id", taxIds.get(0));
          putIfNotEmpty(fields, "seller_tax_id", taxIds.get(1));
        }
        List<String> companyNames = regexExtractAll(text,
          "(?m)^\\s*([^\\r\\n]{2,80}(?:\u80a1\u4efd\u6709\u9650\u516c\u53f8|\u6709\u9650\u516c\u53f8|\u5206\u516c\u53f8))\\s*$");
        if (companyNames.size() < 2) {
          companyNames = regexExtractAll(text,
            "([\u4e00-\u9fffA-Za-z0-9\uff08\uff09()·]{2,80}?(?:\u80a1\u4efd\u6709\u9650\u516c\u53f8|\u6709\u9650\u8d23\u4efb\u516c\u53f8|\u6709\u9650\u516c\u53f8|\u5206\u516c\u53f8))");
        }
        if (companyNames.size() >= 2) {
          putIfNotEmpty(fields, "buyer_name", companyNames.get(0).trim());
          putIfNotEmpty(fields, "seller_name", companyNames.get(1).trim());
        }
      }
      if (isEmpty(fields.get("buyer_name")) || isEmpty(fields.get("seller_name"))
        || isEmpty(fields.get("buyer_tax_id")) || isEmpty(fields.get("seller_tax_id"))) {
        List<String> taxIds = regexExtractAll(text, "(?<![0-9A-Z])([0-9A-Z]{18})(?![0-9A-Z])");
        if (taxIds.size() >= 2) {
          if (isEmpty(fields.get("buyer_tax_id"))) fields.put("buyer_tax_id", taxIds.get(0));
          if (isEmpty(fields.get("seller_tax_id"))) fields.put("seller_tax_id", taxIds.get(1));
        }
        List<String> companyNames = regexExtractAll(text,
          "(?m)^\\s*([^\\r\\n]{2,80}(?:\u80a1\u4efd\u6709\u9650\u516c\u53f8|\u6709\u9650\u516c\u53f8|\u5206\u516c\u53f8))\\s*$");
        if (companyNames.size() < 2) {
          companyNames = regexExtractAll(text,
            "([\u4e00-\u9fffA-Za-z0-9\uff08\uff09()·]{2,80}?(?:\u80a1\u4efd\u6709\u9650\u516c\u53f8|\u6709\u9650\u8d23\u4efb\u516c\u53f8|\u6709\u9650\u516c\u53f8|\u5206\u516c\u53f8))");
        }
        if (companyNames.size() >= 2) {
          if (isEmpty(fields.get("buyer_name"))) fields.put("buyer_name", companyNames.get(0).trim());
          if (isEmpty(fields.get("seller_name"))) fields.put("seller_name", companyNames.get(1).trim());
        }
      }
      normalizeNonTaxAmounts(fields, text);
      String currentIssuer = fields.get("issuer");
      if (isEmpty(currentIssuer) || currentIssuer.contains("\u516c\u53f8")
        || currentIssuer.replaceAll("\\s+", "").equals("\u5408\u8ba1")
        || !isLikelyIssuerName(currentIssuer)) {
        int issuerLabelIndex = text.indexOf("\u5f00\u7968\u4eba");
        String totalAmount = fields.get("total_amount");
        if (issuerLabelIndex >= 0 && !isEmpty(totalAmount)) {
          int totalAmountIndex = text.indexOf(totalAmount, issuerLabelIndex);
          if (totalAmountIndex >= 0) {
            List<String> issuerCandidates = regexExtractAll(text.substring(totalAmountIndex + totalAmount.length()),
              "(?m)^\\s*([^\\r\\n\\d\u00a5\uffe5?]{1,20})\\s*$");
            for (String issuerCandidate : issuerCandidates) {
              String candidate = issuerCandidate.trim();
              if (isLikelyIssuerName(candidate)) {
                fields.put("issuer", candidate);
                break;
              }
            }
          }
        }
      }
    }
    normalizeStructuredFields(fields, filename);
    return fields;
  }

  private void normalizeNonTaxAmounts(Map<String, String> fields, String text) {
    if (fields == null || isEmpty(text) || !text.contains("\u4e0d\u5f81\u7a0e")) return;
    String total = fields.get("total_amount");
    String lineAmount = regexExtract(text, "(\\d+(?:\\.\\d{1,2})?)\\s*\u4e0d\u5f81\u7a0e");
    if (isEmpty(total)) total = firstNonBlank(lineAmount, fields.get("amount_before_tax"));
    if (isEmpty(total)) return;
    fields.put("total_amount", normalizeMoney(total));
    fields.put("amount_before_tax", normalizeMoney(total));
    fields.put("tax_amount", "0.00");
    fields.put("tax_rate", "\u4e0d\u5f81\u7a0e");
  }

  private String normalizeMoney(String value) {
    BigDecimal amount = toBigDecimal(value);
    return amount == null ? value : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private void normalizeStructuredFields(Map<String, String> fields, String filename) {
    String total = fields.get("total_amount");
    if (!isEmpty(total) && !total.matches(".*\\d.*")) {
      fields.remove("total_amount");
    }
    if (isEmpty(fields.get("total_amount"))) {
      String amount = fields.get("amount_before_tax");
      String tax = fields.get("tax_amount");
      BigDecimal amountValue = toBigDecimal(amount);
      BigDecimal taxValue = toBigDecimal(tax);
      if (amountValue != null && taxValue != null) {
        fields.put("total_amount", amountValue.add(taxValue).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
      }
    }
    if (isEmpty(fields.get("invoice_no"))) {
      putIfNotEmpty(fields, "invoice_no", regexExtract(filename, "(\\d{12,30})"));
    }
    if (isEmpty(fields.get("date"))) {
      putIfNotEmpty(fields, "date", extractDateFromText(filename));
    }
    String date = fields.get("date");
    if (!isEmpty(date) && date.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}.+")) {
      fields.put("date", date.substring(0, 10));
    }
    String taxRate = fields.get("tax_rate");
    if (!isEmpty(taxRate) && !taxRate.endsWith("%")) {
      BigDecimal rate = toBigDecimal(taxRate);
      if (rate != null && rate.compareTo(BigDecimal.ONE) < 0) {
        fields.put("tax_rate", rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
      }
    } else if (!isEmpty(taxRate)) {
      BigDecimal rate = toBigDecimal(taxRate.replace("%", ""));
      if (rate != null && rate.compareTo(new BigDecimal("100")) > 0) {
        fields.remove("tax_rate");
      }
    }
  }

  private String extractOfdTextCodes(String xml) {
    if (isEmpty(xml)) return "";
    Matcher matcher = Pattern.compile("<[^:>]*:?TextCode\\b[^>]*>(.*?)</[^:>]*:?TextCode>", Pattern.DOTALL).matcher(xml);
    StringBuilder text = new StringBuilder();
    while (matcher.find()) {
      text.append(matcher.group(1).replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim());
    }
    return text.toString();
  }

  private void putFirst(Map<String, String> fields, String target, Map<String, String> source, String... keys) {
    if (!isEmpty(fields.get(target))) return;
    for (String key : keys) {
      String value = source.get(key);
      if (!isEmpty(value)) {
        fields.put(target, value);
        return;
      }
    }
  }

  private void putIfNotEmpty(Map<String, String> fields, String key, String value) {
    if (!isEmpty(value)) fields.put(key, value);
  }

  private void mergeMissing(Map<String, String> target, Map<String, String> source) {
    source.forEach((key, value) -> {
      if (isEmpty(target.get(key)) && !isEmpty(value)) target.put(key, value);
    });
  }

  private String normalizeXmlKey(String key) {
    if (key == null) return "";
    int colon = key.indexOf(':');
    if (colon >= 0) key = key.substring(colon + 1);
    return key.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
  }

  private String getExtension(String filename) {
    if (filename == null) return "";
    int dot = filename.lastIndexOf('.');
    return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
  }

  private String replaceExtension(String filename, String extension) {
    if (filename == null || filename.isBlank()) return "invoice." + extension;
    int dot = filename.lastIndexOf('.');
    return (dot >= 0 ? filename.substring(0, dot) : filename) + "." + extension;
  }

  private String callOcrService(String imageBase64, String filename) throws Exception {
    if (ocrExecutionService != null) {
      return ocrExecutionService.recognizeBase64(imageBase64, filename);
    }
    long started = System.currentTimeMillis();
    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
    Path tempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "reimbursement-ocr");
    Files.createDirectories(tempDir);
    Path tempFile = tempDir.resolve(java.util.UUID.randomUUID() + "_" + filename);
    Files.write(tempFile, imageBytes);
    log.info("OCR temp file saved: {}", tempFile);

    boolean acquired = false;
    try {
      OCR_REQUESTS.acquire();
      acquired = true;
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
        .timeout(OCR_REQUEST_TIMEOUT)
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
        .build();

      log.info("Sending OCR request for {} to {}, timeout={}s", filename, OCR_SERVER_URL + "/ocr", OCR_REQUEST_TIMEOUT.toSeconds());
      java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("OCR service returned status " + response.statusCode() + ", body: " + response.body());
      }
      log.info("OCR request for {} completed in {}ms", filename, System.currentTimeMillis() - started);
      return response.body();
    } finally {
      if (acquired) {
        OCR_REQUESTS.release();
      }
      try {
        Files.deleteIfExists(tempFile);
      } catch (java.io.IOException e) {
        log.warn("Failed to delete OCR temp file: {}", tempFile);
      }
    }
  }
}
