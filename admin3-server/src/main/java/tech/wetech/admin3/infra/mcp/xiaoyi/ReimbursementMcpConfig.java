package tech.wetech.admin3.infra.mcp.xiaoyi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import tech.wetech.admin3.sys.model.ApprovalLog;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.InvoiceTemp;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.repository.ApprovalLogRepository;
import tech.wetech.admin3.sys.repository.AttachmentRepository;
import tech.wetech.admin3.sys.repository.ReimbursementRepository;
import tech.wetech.admin3.sys.service.ReimbursementService;
import tech.wetech.admin3.sys.service.dto.ReimbursementDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Configuration
public class ReimbursementMcpConfig {

  private static final Logger log = LoggerFactory.getLogger(ReimbursementMcpConfig.class);

  private static final String OCR_SERVER_URL = "http://localhost:8765";

  @Bean
  McpJsonMapper reimbursementMcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  @Bean
  WebMvcStatelessServerTransport reimbursementWebMvcStatelessServerTransport(@Qualifier("reimbursementMcpJsonMapper") McpJsonMapper reimbursementMcpJsonMapper) {
    return WebMvcStatelessServerTransport.builder()
      .jsonMapper(reimbursementMcpJsonMapper)
      .messageEndpoint("/xiaoyi-mcp/reimbursement/message")
      .securityValidator(ServerTransportSecurityValidator.NOOP)
      .build();
  }

  @Bean
  RouterFunction<ServerResponse> reimbursementMcpRouterFunction(@Qualifier("reimbursementWebMvcStatelessServerTransport") WebMvcStatelessServerTransport reimbursementWebMvcStatelessServerTransport) {
    return reimbursementWebMvcStatelessServerTransport.getRouterFunction();
  }

  @Bean
  McpStatelessSyncServer reimbursementMcpStatelessSyncServer(
    @Qualifier("reimbursementWebMvcStatelessServerTransport") WebMvcStatelessServerTransport transport,
    ReimbursementService reimbursementService,
    AttachmentRepository attachmentRepository,
    ReimbursementRepository reimbursementRepository,
    ApprovalLogRepository approvalLogRepository,
    @Qualifier("reimbursementMcpJsonMapper") McpJsonMapper jsonMapper) {
    return McpServer.sync(transport)
      .serverInfo("xiaoyi-reimbursement-mcp-server", "1.0.0")
      .capabilities(McpSchema.ServerCapabilities.builder()
        .tools(true)
        .logging()
        .build())
      .tools(
        createOcrUploadTool(reimbursementService, attachmentRepository, jsonMapper),
        createConfirmReimbursementTool(reimbursementService, reimbursementRepository, approvalLogRepository, jsonMapper),
        createQueryReimbursementsTool(reimbursementService, jsonMapper)
      )
      .build();
  }

  private final Path uploadDir = Paths.get("uploads/attachments");

  /**
   * Tool 1: 上传图片完成 OCR，保存到发票草稿，返回识别数据给客户确认
   */
  private McpStatelessServerFeatures.SyncToolSpecification createOcrUploadTool(ReimbursementService reimbursementService, AttachmentRepository attachmentRepository, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("ocr_upload_reimbursement")
      .description("上传发票图片进行OCR识别，保存为草稿报销单，返回识别出的发票字段供用户确认。用户确认后请调用 confirm_reimbursement 完成创建。")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "imageBase64": {
              "type": "string",
              "description": "图片的 Base64 编码数据"
            },
            "filename": {
              "type": "string",
              "description": "文件名，例如 invoice.jpg"
            },
            "applicantName": {
              "type": "string",
              "description": "报销申请人姓名"
            }
          },
          "required": ["imageBase64", "filename", "applicantName"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      try {
        String imageBase64 = String.valueOf(request.arguments().get("imageBase64"));
        String filename = String.valueOf(request.arguments().get("filename"));
        String applicantName = String.valueOf(request.arguments().get("applicantName"));

        long tStart = System.currentTimeMillis();
        log.info("Reimbursement MCP tool called: ocr_upload_reimbursement, filename={}, applicantName={}", filename, applicantName);

        // 1. 解码 Base64 图片
        long t1 = System.currentTimeMillis();
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        log.info("[耗时] Base64解码: {}ms", System.currentTimeMillis() - t1);

        // 2. 调用本地 OCR 服务识别发票
        long t2 = System.currentTimeMillis();
        String ocrResultJson = callOcrService(imageBytes, filename);
        log.info("[耗时] OCR服务调用: {}ms", System.currentTimeMillis() - t2);

        // 3. 解析 OCR 结果，提取发票字段
        long t3 = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        var ocrResult = mapper.readTree(ocrResultJson);

        if (!"ok".equals(ocrResult.get("status").asText())) {
          String error = ocrResult.has("error") ? ocrResult.get("error").asText() : "OCR识别失败";
          return new McpSchema.CallToolResult("OCR识别失败: " + error, true);
        }

        // 提取发票字段
        String invoiceNo = "";
        String invoiceCode = "";
        String totalAmount = "0";
        String date = "";
        String buyerName = "";
        String sellerName = "";
        String buyerTaxId = "";
        String sellerTaxId = "";
        String invoiceType = "";

        // 优先从 invoice_fields 取
        var invoiceFields = ocrResult.get("invoice_fields");
        if (invoiceFields != null && !invoiceFields.isEmpty()) {
          if (invoiceFields.has("invoice_no")) invoiceNo = invoiceFields.get("invoice_no").asText();
          if (invoiceFields.has("invoice_code")) invoiceCode = invoiceFields.get("invoice_code").asText();
          if (invoiceFields.has("total_amount")) totalAmount = invoiceFields.get("total_amount").asText();
          if (invoiceFields.has("date")) date = invoiceFields.get("date").asText();
          if (invoiceFields.has("buyer_name")) buyerName = invoiceFields.get("buyer_name").asText();
          if (invoiceFields.has("seller_name")) sellerName = invoiceFields.get("seller_name").asText();
          if (invoiceFields.has("buyer_tax_id")) buyerTaxId = invoiceFields.get("buyer_tax_id").asText();
          if (invoiceFields.has("seller_tax_id")) sellerTaxId = invoiceFields.get("seller_tax_id").asText();
          if (invoiceFields.has("invoice_type")) invoiceType = invoiceFields.get("invoice_type").asText();
        }

        // 如果 KIE 没提取到，从 ocr_text 中正则提取
        String ocrText = ocrResult.has("ocr_text") ? ocrResult.get("ocr_text").asText() : "";
        log.info("[耗时] 解析OCR结果+提取字段: {}ms, invoice_fields={}, ocr_text_len={}", System.currentTimeMillis() - t3, invoiceFields != null ? invoiceFields.size() : 0, ocrText.length());
        if (invoiceNo.isEmpty() && ocrText.contains("发票号码")) {
          var m = java.util.regex.Pattern.compile("发票号码[：:\\s]*(\\d{8})").matcher(ocrText);
          if (m.find()) invoiceNo = m.group(1);
        }
        if (invoiceCode.isEmpty() && ocrText.contains("发票代码")) {
          var m = java.util.regex.Pattern.compile("发票代码[：:\\s]*(\\d{12})").matcher(ocrText);
          if (m.find()) invoiceCode = m.group(1);
        }
        if (date.isEmpty() && ocrText.contains("开票日期")) {
          var m = java.util.regex.Pattern.compile("开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)").matcher(ocrText);
          if (m.find()) date = m.group(1);
        }
        if (buyerName.isEmpty() && ocrText.contains("购买方")) {
          int idx = ocrText.indexOf("购买方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m2 = java.util.regex.Pattern.compile("称[：:\\s]*([^\\n]{2,50})").matcher(after);
            if (m2.find()) buyerName = m2.group(1).trim();
          }
        }
        if (sellerName.isEmpty() && ocrText.contains("销售方")) {
          int idx = ocrText.indexOf("销售方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m2 = java.util.regex.Pattern.compile("称[：:\\s]*([^\\n]{2,50})").matcher(after);
            if (m2.find()) sellerName = m2.group(1).trim();
          }
        }
        if (buyerTaxId.isEmpty() && ocrText.contains("购买方")) {
          int idx = ocrText.indexOf("购买方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m2 = java.util.regex.Pattern.compile("纳税人识别号[：:\\s]*([\\dA-Za-z]{15,20})").matcher(after);
            if (m2.find()) buyerTaxId = m2.group(1).trim();
          }
        }
        if (sellerTaxId.isEmpty() && ocrText.contains("销售方")) {
          int idx = ocrText.indexOf("销售方");
          if (idx >= 0) {
            String after = ocrText.substring(idx);
            var m2 = java.util.regex.Pattern.compile("纳税人识别号[：:\\s]*([\\dA-Za-z]{15,20})").matcher(after);
            if (m2.find()) sellerTaxId = m2.group(1).trim();
          }
        }
        if (invoiceType.isEmpty()) {
          if (ocrText.contains("增值税专用发票")) invoiceType = "special";
          else if (ocrText.contains("增值税普通发票")) invoiceType = "normal";
          else if (ocrText.contains("电子发票") || ocrText.contains("电子普通发票")) invoiceType = "electronic";
          else if (ocrText.contains("定额发票")) invoiceType = "fixed";
          else if (ocrText.contains("机动车")) invoiceType = "vehicle";
        }
        if ("0".equals(totalAmount) && ocrText.contains("小写")) {
          var m = java.util.regex.Pattern.compile("[（(]小写[)）][：:\\s]*[¥￥]?(\\d+\\.?\\d*)").matcher(ocrText);
          if (m.find()) totalAmount = m.group(1);
        }

        // 解析开票日期
        LocalDateTime invoiceDate = null;
        if (!date.isEmpty()) {
          try {
            var formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
            invoiceDate = LocalDate.parse(date, formatter).atStartOfDay();
          } catch (DateTimeParseException ignored) {
          }
        }

        // 3. 创建草稿报销单（状态为 draft）
        // 标题加 [MCP] 前缀，与 Web 端创建的报销单区分
        String title = "[MCP] 发票报销 - " + (invoiceNo.isEmpty() ? filename : invoiceNo);
        BigDecimal amount;
        try {
          amount = new BigDecimal(totalAmount);
        } catch (NumberFormatException e) {
          amount = BigDecimal.ZERO;
        }
        // description 开头标记来源，方便区分 MCP 与 Web 创建的报销单
        String description = "[来源: MCP]\n"
          + "OCR识别自动创建\n"
          + "发票号码: " + (invoiceNo.isEmpty() ? "未识别" : invoiceNo) + "\n"
          + "发票代码: " + (invoiceCode.isEmpty() ? "未识别" : invoiceCode) + "\n"
          + "开票日期: " + (date.isEmpty() ? "未识别" : date) + "\n"
          + "购买方: " + (buyerName.isEmpty() ? "未识别" : buyerName) + "\n"
          + "销售方: " + (sellerName.isEmpty() ? "未识别" : sellerName) + "\n"
          + "购买方税号: " + (buyerTaxId.isEmpty() ? "未识别" : buyerTaxId) + "\n"
          + "销售方税号: " + (sellerTaxId.isEmpty() ? "未识别" : sellerTaxId) + "\n"
          + "发票类型: " + (invoiceType.isEmpty() ? "未识别" : invoiceType) + "\n"
          + "原始OCR文本: " + (ocrText.length() > 200 ? ocrText.substring(0, 200) + "..." : ocrText);

        // MCP 上下文没有 Session，使用调用方传入的用户信息创建草稿报销单
        // 同时传入 OCR 识别的发票字段，确保报销单正式字段有数据
        long t4 = System.currentTimeMillis();
        ReimbursementDTO draft = reimbursementService.createReimbursement(
          title, "other", amount, description, null, 0L, applicantName,
          invoiceNo.isEmpty() ? null : invoiceNo,
          invoiceCode.isEmpty() ? null : invoiceCode,
          invoiceDate,
          buyerName.isEmpty() ? null : buyerName,
          sellerName.isEmpty() ? null : sellerName,
          buyerTaxId.isEmpty() ? null : buyerTaxId,
          sellerTaxId.isEmpty() ? null : sellerTaxId,
          invoiceType.isEmpty() ? null : invoiceType,
          "normal",
          ocrResultJson
        );
        log.info("[耗时] 创建草稿报销单: {}ms, draft_id={}", System.currentTimeMillis() - t4, draft.id());

        // 4. 保存图片到附件目录并创建 Attachment 记录
        long t5 = System.currentTimeMillis();
        try {
          Files.createDirectories(uploadDir);
          String ext = "";
          if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf("."));
          }
          String storedName = UUID.randomUUID().toString() + ext;
          Path targetPath = uploadDir.resolve(storedName);
          Files.write(targetPath, imageBytes);

          Attachment attachment = new Attachment();
          attachment.setReimbursementId(draft.id());
          attachment.setFileName(filename);
          attachment.setFileSize((long) imageBytes.length);
          attachment.setFileType("image/" + (ext.isEmpty() ? "png" : ext.substring(1)));
          attachment.setFileUrl("/attachments/" + storedName + "/download");
          attachment.setUploadedBy(0L);
          attachment.setCreatedAt(LocalDateTime.now());
          attachment.setOcrStatus("processing");
          attachmentRepository.save(attachment);
          log.info("Attachment saved for reimbursement {}: {}", draft.id(), storedName);

          // OCR 完成后更新附件状态
          attachment.setOcrStatus("completed");
          attachment.setOcrResult(ocrResultJson);
          attachmentRepository.save(attachment);
        } catch (IOException e) {
          log.warn("Failed to save attachment for reimbursement {}: {}", draft.id(), e.getMessage());
        }
        log.info("[耗时] 保存附件: {}ms", System.currentTimeMillis() - t5);

        // 5. 创建临时发票记录，保存 OCR 识别结果
        long t6 = System.currentTimeMillis();
        InvoiceTemp invoiceTemp = reimbursementService.createInvoiceTemp(draft.id(), null, 0L);
        reimbursementService.updateInvoiceTempOcrResult(
          invoiceTemp.getId(), ocrResultJson,
          invoiceNo.isEmpty() ? null : invoiceNo,
          invoiceCode.isEmpty() ? null : invoiceCode,
          invoiceDate,
          buyerName.isEmpty() ? null : buyerName,
          sellerName.isEmpty() ? null : sellerName,
          buyerTaxId.isEmpty() ? null : buyerTaxId,
          sellerTaxId.isEmpty() ? null : sellerTaxId,
          amount,
          invoiceType.isEmpty() ? null : invoiceType,
          "normal",
          0L
        );
        log.info("[耗时] 创建临时发票记录: {}ms, invoice_temp_id={}", System.currentTimeMillis() - t6, invoiceTemp.getId());

        // 6. 返回识别结果给用户确认
        long tTotal = System.currentTimeMillis() - tStart;
        log.info("[耗时] ocr_upload_reimbursement 总耗时: {}ms", tTotal);
        String result = String.format("""
          OCR识别完成，已创建草稿报销单，请确认以下信息：

          【草稿报销单 ID: %s】
          【临时发票 ID: %s】
          标题: %s
          金额: ¥%s
          发票号码: %s
          发票代码: %s
          开票日期: %s
          购买方: %s
          销售方: %s
          购买方税号: %s
          销售方税号: %s
          发票类型: %s

          【OCR原始文本】
          %s

          请确认无误后，调用 confirm_reimbursement 工具，传入报销单ID: %s 完成创建。
          如果不满意，可以再次上传图片重新识别（会创建新的临时发票记录）。
          """,
          draft.id(),
          invoiceTemp.getId(),
          title,
          amount,
          invoiceNo.isEmpty() ? "未识别" : invoiceNo,
          invoiceCode.isEmpty() ? "未识别" : invoiceCode,
          date.isEmpty() ? "未识别" : date,
          buyerName.isEmpty() ? "未识别" : buyerName,
          sellerName.isEmpty() ? "未识别" : sellerName,
          buyerTaxId.isEmpty() ? "未识别" : buyerTaxId,
          sellerTaxId.isEmpty() ? "未识别" : sellerTaxId,
          invoiceType.isEmpty() ? "未识别" : invoiceType,
          ocrText.length() > 500 ? ocrText.substring(0, 500) + "..." : ocrText,
          draft.id()
        );
        return new McpSchema.CallToolResult(result, false);
      } catch (Exception e) {
        log.error("Reimbursement MCP ocr_upload_reimbursement failed: {}", e.getMessage(), e);
        return new McpSchema.CallToolResult("OCR上传识别失败: " + e.getMessage(), true);
      }
    });
  }

  /**
   * Tool 2: 用户确认后，将草稿报销单转为正式报销单（提交审批）
   * <p>
   * 注意：此方法直接操作数据库，不走 ReimbursementService.submitReimbursement，
   * 避免依赖 SessionItemHolder 会话上下文。审批日志使用虚拟用户 xiaoyi_mcp。
   */
  private McpStatelessServerFeatures.SyncToolSpecification createConfirmReimbursementTool(ReimbursementService reimbursementService,
                                                                                          ReimbursementRepository reimbursementRepository,
                                                                                          ApprovalLogRepository approvalLogRepository,
                                                                                          McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("confirm_reimbursement")
      .description("用户确认OCR识别结果后，将草稿报销单提交为正式报销单（进入审批流程）")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "reimbursementId": {
              "type": "number",
              "description": "草稿报销单ID（从 ocr_upload_reimbursement 返回中获取）"
            },
            "title": {
              "type": "string",
              "description": "报销标题（可选，不传则使用草稿中的标题）"
            },
            "category": {
              "type": "string",
              "description": "报销分类: travel(差旅), office(办公), entertainment(招待), other(其他)"
            },
            "amount": {
              "type": "string",
              "description": "报销金额（可选，不传则使用OCR识别的金额）"
            }
          },
          "required": ["reimbursementId", "category"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      long startTime = System.currentTimeMillis();
      try {
        Long reimbursementId = Long.valueOf(String.valueOf(request.arguments().get("reimbursementId")));
        String category = String.valueOf(request.arguments().get("category"));
        String title = request.arguments().containsKey("title") ? String.valueOf(request.arguments().get("title")) : null;
        String amountStr = request.arguments().containsKey("amount") ? String.valueOf(request.arguments().get("amount")) : null;

        log.info("[xiaoyi_mcp] confirm_reimbursement called: reimbursementId={}, category={}, title={}, amount={}",
          reimbursementId, category, title, amountStr);

        // 获取草稿报销单
        ReimbursementDTO draft = reimbursementService.findReimbursementById(reimbursementId);
        if (!"draft".equals(draft.status())) {
          log.warn("[xiaoyi_mcp] confirm_reimbursement failed: not draft status, id={}, status={}", reimbursementId, draft.status());
          return new McpSchema.CallToolResult("该报销单不是草稿状态，无法提交", true);
        }

        // 更新报销信息
        if (title == null) title = draft.title();
        BigDecimal amount;
        if (amountStr != null) {
          try {
            amount = new BigDecimal(amountStr);
          } catch (NumberFormatException e) {
            amount = draft.amount();
          }
        } else {
          amount = draft.amount();
        }

        // 更新报销单基本信息
        reimbursementService.updateReimbursement(reimbursementId, title, category, amount, draft.description(), null);

        // 确认最新的待确认临时发票，将 OCR 数据同步到正式报销单
        var invoiceTemps = reimbursementService.findInvoiceTempsByReimbursementId(reimbursementId);
        var pendingTemp = invoiceTemps.stream()
          .filter(t -> "pending".equals(t.status()))
          .findFirst();
        if (pendingTemp.isPresent()) {
          reimbursementService.confirmInvoiceTemp(pendingTemp.get().id());
        }

        // 直接操作数据库提交报销单，不依赖 SessionItemHolder
        Reimbursement r = reimbursementRepository.findById(reimbursementId)
          .orElseThrow(() -> new RuntimeException("报销单不存在: " + reimbursementId));
        if (!"draft".equals(r.getStatus())) {
          return new McpSchema.CallToolResult("仅草稿状态可提交", true);
        }
        if (r.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
          return new McpSchema.CallToolResult("报销金额必须大于0", true);
        }
        r.setStatus("pending");
        r.setUpdatedAt(LocalDateTime.now());
        reimbursementRepository.save(r);

        // 写入审批日志（使用虚拟用户 xiaoyi_mcp）
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setReimbursementId(reimbursementId);
        approvalLog.setAction("submit");
        approvalLog.setOperatorId(0L);
        approvalLog.setOperatorName("xiaoyi_mcp");
        approvalLog.setCreatedAt(LocalDateTime.now());
        approvalLogRepository.save(approvalLog);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[xiaoyi_mcp] confirm_reimbursement succeeded: id={}, title={}, category={}, amount={}, applicantName={}, elapsed={}ms",
          r.getId(), r.getTitle(), r.getCategory(), r.getAmount(), r.getApplicantName(), elapsed);

        String result = String.format("""
          报销单提交成功！已进入审批流程。

          【报销单信息】
          ID: %s
          标题: %s
          分类: %s
          金额: ¥%s
          状态: %s
          申请人: %s
          创建时间: %s

          请等待审批。
          """,
          r.getId(),
          r.getTitle(),
          r.getCategory(),
          r.getAmount(),
          "待审批",
          r.getApplicantName(),
          r.getCreatedAt() != null ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""
        );
        return new McpSchema.CallToolResult(result, false);
      } catch (Exception e) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.error("[xiaoyi_mcp] confirm_reimbursement failed: id={}, elapsed={}ms, error={}", request.arguments().get("reimbursementId"), elapsed, e.getMessage(), e);
        return new McpSchema.CallToolResult("提交报销单失败: " + e.getMessage(), true);
      }
    });
  }

  /**
   * Tool 3: 按用户姓名查询个人报销单
   */
  private McpStatelessServerFeatures.SyncToolSpecification createQueryReimbursementsTool(ReimbursementService reimbursementService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("query_reimbursements_by_username")
      .description("按用户姓名查询个人报销单列表，返回报销单的详细信息")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "username": {
              "type": "string",
              "description": "用户名"
            }
          },
          "required": ["username"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      String username = String.valueOf(request.arguments().get("username"));
      log.info("Reimbursement MCP tool called: query_reimbursements_by_username, username={}", username);
      List<ReimbursementDTO> list = reimbursementService.findReimbursementsByUsername(username);
      log.info("Reimbursement MCP query_reimbursements_by_username result: username={}, count={}", username, list.size());

      if (list.isEmpty()) {
        return new McpSchema.CallToolResult("未找到该用户的报销记录", false);
      }

      StringBuilder result = new StringBuilder();
      for (var r : list) {
        String statusLabel = switch (r.status()) {
          case "draft" -> "草稿";
          case "pending" -> "待审批";
          case "approved" -> "已通过";
          case "rejected" -> "被退回";
          case "recalled" -> "已撤回";
          default -> r.status();
        };
        result.append(String.format("""
          报销记录:
          ID: %s
          标题: %s
          分类: %s
          金额: ¥%s
          状态: %s
          申请人: %s
          创建时间: %s
          --------------------
          """,
          r.id(),
          r.title(),
          r.category(),
          r.amount(),
          statusLabel,
          r.applicantName(),
          r.createdAt() != null ? r.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""
        ));
      }
      return new McpSchema.CallToolResult(result.toString(), false);
    });
  }

  /**
   * 调用本地 OCR 服务的 HTTP REST 接口识别发票
   *
   * 流程: 保存 Base64 图片到临时文件 → 调用 POST /ocr 上传文件 → 返回识别结果 JSON
   */
  private String callOcrService(byte[] imageBytes, String filename) throws Exception {
    long t0 = System.currentTimeMillis();

    // 1. 将图片数据保存到临时文件
    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "reimbursement-ocr");
    Files.createDirectories(tempDir);
    Path tempFile = tempDir.resolve(UUID.randomUUID().toString() + "_" + filename);
    Files.write(tempFile, imageBytes);
    log.info("[耗时] OCR保存临时文件: {}ms, path={}", System.currentTimeMillis() - t0, tempFile);

    try {
      // 2. 构建 multipart/form-data 请求体
      long t1 = System.currentTimeMillis();
      String boundary = "----" + UUID.randomUUID().toString();
      String lineSeparator = "\r\n";

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      // 文件部分
      baos.write(("--" + boundary + lineSeparator).getBytes());
      baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + lineSeparator).getBytes());
      baos.write(("Content-Type: application/octet-stream" + lineSeparator).getBytes());
      baos.write(lineSeparator.getBytes());
      baos.write(imageBytes);
      baos.write(lineSeparator.getBytes());
      // 结束
      baos.write(("--" + boundary + "--" + lineSeparator).getBytes());
      log.info("[耗时] OCR构建请求体: {}ms, body_size={}", System.currentTimeMillis() - t1, baos.size());

      // 3. 发送 HTTP POST 请求（带超时）
      long t2 = System.currentTimeMillis();
      HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(OCR_SERVER_URL + "/ocr"))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .timeout(Duration.ofSeconds(60))
        .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
        .build();

      log.info("Sending OCR request to {}...", OCR_SERVER_URL + "/ocr");
      HttpResponse<String> response;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (java.net.ConnectException e) {
        log.error("OCR service connection refused: {}", e.getMessage());
        throw new RuntimeException("OCR服务连接失败，请确认OCR服务已启动 (http://localhost:8765)", e);
      } catch (java.net.http.HttpTimeoutException e) {
        log.error("OCR service timeout: {}", e.getMessage());
        throw new RuntimeException("OCR服务连接超时，请确认OCR服务运行正常", e);
      }
      long tHttp = System.currentTimeMillis() - t2;
      String responseBody = response.body();

      log.info("[耗时] OCR HTTP请求: {}ms, status={}, response_size={}", tHttp, response.statusCode(), responseBody.length());

      if (response.statusCode() != 200) {
        throw new RuntimeException("OCR服务返回错误状态: " + response.statusCode() + ", body: " + responseBody);
      }

      log.info("[耗时] callOcrService 总耗时: {}ms", System.currentTimeMillis() - t0);
      return responseBody;
    } finally {
      // 4. 清理临时文件
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Failed to delete OCR temp file: {}", tempFile);
      }
    }
  }
}