package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * OCR 识别服务，封装对本地 OCR MCP 服务的调用
 */
@Service
public class OcrService {

  private static final Logger log = LoggerFactory.getLogger(OcrService.class);

  private static final String OCR_SERVER_URL = "http://localhost:8765";

  private final AttachmentRepository attachmentRepository;
  private final ReimbursementRepository reimbursementRepository;
  private final ObjectMapper objectMapper;

  public OcrService(AttachmentRepository attachmentRepository, ReimbursementRepository reimbursementRepository, ObjectMapper objectMapper) {
    this.attachmentRepository = attachmentRepository;
    this.reimbursementRepository = reimbursementRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * 对附件执行 OCR 识别（异步调用），更新附件状态和报销单字段
   */
  public void recognizeAttachmentAsync(Long attachmentId, Path imagePath) {
    Thread.startVirtualThread(() -> {
      try {
        recognizeAttachment(attachmentId, imagePath);
      } catch (Exception e) {
        log.error("OCR async recognition failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      }
    });
  }

  /**
   * 对附件执行 OCR 识别（同步调用），更新附件状态和报销单字段
   */
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
      // 读取图片文件并转为 Base64
      byte[] imageBytes = Files.readAllBytes(imagePath);
      String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

      // 调用 OCR 服务
      String ocrResultJson = callOcrService(base64, attachment.getFileName());
      log.info("OCR completed for attachment {}, result length: {}", attachmentId, ocrResultJson.length());

      // 更新附件状态
      attachment.setOcrStatus("completed");
      attachment.setOcrResult(ocrResultJson);
      attachmentRepository.save(attachment);
      log.info("Attachment {} OCR status updated to completed", attachmentId);

      // 自动更新关联的报销单字段
      updateReimbursementFromOcr(attachment.getReimbursementId(), ocrResultJson);
    } catch (Exception e) {
      log.error("OCR failed for attachment {}: {}", attachmentId, e.getMessage(), e);
      attachment.setOcrStatus("failed");
      attachment.setOcrResult("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
      attachmentRepository.save(attachment);
    }
  }

  /**
   * 从 OCR 结果中提取发票字段，自动更新报销单
   */
  private void updateReimbursementFromOcr(Long reimbursementId, String ocrResultJson) {
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
      var root = objectMapper.readTree(ocrResultJson);
      // 解析 MCP 响应格式：result.content[0].text
      String innerText = null;
      if (root.has("result") && root.get("result").has("content") && root.get("result").get("content").isArray()) {
        var content = root.get("result").get("content").get(0);
        if (content != null && content.has("text")) {
          innerText = content.get("text").asText();
        }
      }
      // 尝试解析内层 JSON
      JsonNode data;
      if (innerText != null) {
        try {
          data = objectMapper.readTree(innerText);
        } catch (Exception e) {
          data = root;
        }
      } else {
        data = root;
      }

      String invoiceNo = getJsonField(data, "invoice_no");
      String invoiceCode = getJsonField(data, "invoice_code");
      String totalAmount = getJsonField(data, "total_amount");
      String date = getJsonField(data, "date");
      String buyerName = getJsonField(data, "buyer_name");
      String sellerName = getJsonField(data, "seller_name");
      String buyerTaxId = getJsonField(data, "buyer_tax_id");
      String sellerTaxId = getJsonField(data, "seller_tax_id");
      String invoiceType = getJsonField(data, "invoice_type");

      // 从 ocr_text 正则提取补充
      String ocrText = data.has("ocr_text") ? data.get("ocr_text").asText() : "";
      log.debug("OCR ocr_text length: {}, preview: {}", ocrText != null ? ocrText.length() : 0,
        ocrText != null ? ocrText.substring(0, Math.min(200, ocrText.length())).replace("\n", "\\n") : "null");
      if (ocrText != null && !ocrText.isEmpty()) {
        if (isEmpty(invoiceNo)) invoiceNo = regexExtract(ocrText, "发票号码[：:\\s]*(\\d{8})");
        if (isEmpty(invoiceCode)) invoiceCode = regexExtract(ocrText, "发票代码[：:\\s]*(\\d{12})");
        if (isEmpty(date)) date = regexExtract(ocrText, "开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)");
        if (isEmpty(buyerName)) {
          int idx = ocrText.indexOf("购买方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m = Pattern.compile("称[：:\\s]*([^\\n]{2,50})").matcher(after);
            if (m.find()) buyerName = m.group(1).trim();
          }
        }
        if (isEmpty(sellerName)) {
          int idx = ocrText.indexOf("销售方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m = Pattern.compile("称[：:\\s]*([^\\n]{2,50})").matcher(after);
            if (m.find()) sellerName = m.group(1).trim();
          }
        }
        if (isEmpty(buyerTaxId)) {
          int idx = ocrText.indexOf("购买方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m = Pattern.compile("纳税人识别号[：:\\s]*([\\dA-Za-z]{15,20})").matcher(after);
            if (m.find()) buyerTaxId = m.group(1).trim();
          }
        }
        if (isEmpty(sellerTaxId)) {
          int idx = ocrText.indexOf("销售方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m = Pattern.compile("纳税人识别号[：:\\s]*([\\dA-Za-z]{15,20})").matcher(after);
            if (m.find()) sellerTaxId = m.group(1).trim();
          }
        }
        if (isEmpty(invoiceType)) {
          if (ocrText.contains("增值税专用发票")) invoiceType = "special";
          else if (ocrText.contains("增值税普通发票")) invoiceType = "normal";
          else if (ocrText.contains("电子发票")) invoiceType = "electronic";
        }
        if (isEmpty(totalAmount) || "0".equals(totalAmount)) {
          log.debug("Attempting to extract total_amount from ocr_text via regex");
          var m = Pattern.compile("[（(]小写[)）][：:\\s]*[¥￥]?(\\d+\\.?\\d*)").matcher(ocrText);
          if (m.find()) {
            totalAmount = m.group(1);
            log.debug("Regex extracted total_amount: {}", totalAmount);
          } else {
            log.debug("Regex did not match total_amount in ocr_text");
            // 尝试备选正则：匹配"合计"后面的金额
            var m2 = Pattern.compile("合计[\\s\\S]{0,20}[¥￥]?(\\d+\\.\\d{2})").matcher(ocrText);
            if (m2.find()) {
              totalAmount = m2.group(1);
              log.debug("Fallback regex extracted total_amount: {}", totalAmount);
            }
          }
        }
      }

      // 更新报销单字段
      boolean updated = false;
      if (!isEmpty(invoiceNo)) { r.setInvoiceNo(invoiceNo); updated = true; }
      if (!isEmpty(invoiceCode)) { r.setInvoiceCode(invoiceCode); updated = true; }
      if (!isEmpty(buyerName)) { r.setBuyerName(buyerName); updated = true; }
      if (!isEmpty(sellerName)) { r.setSellerName(sellerName); updated = true; }
      if (!isEmpty(buyerTaxId)) { r.setBuyerTaxId(buyerTaxId); updated = true; }
      if (!isEmpty(sellerTaxId)) { r.setSellerTaxId(sellerTaxId); updated = true; }
      if (!isEmpty(invoiceType)) { r.setInvoiceType(invoiceType); updated = true; }
      if (!isEmpty(totalAmount)) {
        try { r.setAmount(new BigDecimal(totalAmount)); updated = true; } catch (NumberFormatException ignored) {}
      }
      if (!isEmpty(date)) {
        try {
          var formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
          r.setInvoiceDate(LocalDate.parse(date, formatter).atStartOfDay());
          updated = true;
        } catch (DateTimeParseException ignored) {}
      }

      // 更新说明（补全所有匹配到的字段）
      StringBuilder desc = new StringBuilder();
      if (!isEmpty(invoiceNo)) desc.append("发票号码: ").append(invoiceNo).append("\n");
      if (!isEmpty(invoiceCode)) desc.append("发票代码: ").append(invoiceCode).append("\n");
      if (!isEmpty(date)) desc.append("开票日期: ").append(date).append("\n");
      if (!isEmpty(buyerName)) desc.append("购买方: ").append(buyerName).append("\n");
      if (!isEmpty(sellerName)) desc.append("销售方: ").append(sellerName).append("\n");
      if (!isEmpty(buyerTaxId)) desc.append("购买方税号: ").append(buyerTaxId).append("\n");
      if (!isEmpty(sellerTaxId)) desc.append("销售方税号: ").append(sellerTaxId).append("\n");
      if (!isEmpty(invoiceType)) desc.append("发票类型: ").append(invoiceType).append("\n");
      if (!isEmpty(totalAmount)) desc.append("金额: ¥").append(totalAmount).append("\n");
      if (desc.length() > 0) {
        r.setDescription(desc.toString().trim());
        updated = true;
      }

      // 保存完整 OCR 原始 JSON 到报销单
      r.setOcrRawJson(ocrResultJson);
      updated = true;

      if (updated) {
        r.setUpdatedAt(LocalDateTime.now());
        reimbursementRepository.save(r);
        log.info("Reimbursement {} auto-updated from OCR result: invoiceNo={}, sellerName={}, amount={}",
          reimbursementId, invoiceNo, sellerName, totalAmount);
      }
    } catch (Exception e) {
      log.error("Failed to auto-update reimbursement {} from OCR result: {}", reimbursementId, e.getMessage(), e);
    }
  }

  private String getJsonField(JsonNode node, String field) {
    if (node.has("invoice_fields") && node.get("invoice_fields").has(field)) {
      return node.get("invoice_fields").get(field).asText();
    }
    return "";
  }

  private String regexExtract(String text, String pattern) {
    var m = Pattern.compile(pattern).matcher(text);
    return m.find() ? m.group(1) : "";
  }

  private boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

  /**
   * 调用本地 OCR 服务的 HTTP REST 接口识别发票
   * 通过 multipart/form-data 上传图片文件
   */
  private String callOcrService(String imageBase64, String filename) throws Exception {
    // 1. 将 Base64 图片数据保存到临时文件
    byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
    Path tempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "reimbursement-ocr");
    java.nio.file.Files.createDirectories(tempDir);
    Path tempFile = tempDir.resolve(java.util.UUID.randomUUID().toString() + "_" + filename);
    java.nio.file.Files.write(tempFile, imageBytes);
    log.info("OCR temp file saved: {}", tempFile);

    try {
      // 2. 构建 multipart/form-data 请求体
      String boundary = "----" + java.util.UUID.randomUUID().toString();
      String lineSeparator = "\r\n";

      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      baos.write(("--" + boundary + lineSeparator).getBytes());
      baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + lineSeparator).getBytes());
      baos.write(("Content-Type: application/octet-stream" + lineSeparator).getBytes());
      baos.write(lineSeparator.getBytes());
      baos.write(imageBytes);
      baos.write(lineSeparator.getBytes());
      baos.write(("--" + boundary + "--" + lineSeparator).getBytes());

      // 3. 发送 HTTP POST 请求
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(OCR_SERVER_URL + "/ocr"))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
        .build();

      java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();

      log.info("OCR service response status: {}", response.statusCode());

      if (response.statusCode() != 200) {
        throw new RuntimeException("OCR服务返回错误状态: " + response.statusCode() + ", body: " + responseBody);
      }

      return responseBody;
    } finally {
      // 4. 清理临时文件
      try {
        java.nio.file.Files.deleteIfExists(tempFile);
      } catch (java.io.IOException e) {
        log.warn("Failed to delete OCR temp file: {}", tempFile);
      }
    }
  }
}
