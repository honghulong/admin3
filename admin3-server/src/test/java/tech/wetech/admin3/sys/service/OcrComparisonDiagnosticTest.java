package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OcrComparisonDiagnosticTest {

  private static final Path SAMPLE_DIR = Path.of(
    "D:\\hongmengProjects\\admin3\\docs\\test2\\20260727报销单\\20260727报销单");
  private static final Path OUTPUT_DIR = Path.of(
    "D:\\hongmengProjects\\admin3\\docs\\test2\\ocr-diagnostic-20260729");
  private static final List<String> FIELD_NAMES = List.of(
    "invoice_type", "invoice_no", "invoice_code", "date",
    "buyer_name", "buyer_tax_id", "seller_name", "seller_tax_id",
    "total_amount", "amount_before_tax", "tax_amount", "tax_rate",
    "payee", "reviewer", "issuer", "remarks"
  );
  private static final List<String> REGION_FIELD_NAMES = List.of(
    "invoice_no", "date", "buyer_name", "buyer_tax_id", "seller_name", "seller_tax_id",
    "total_amount", "amount_before_tax", "tax_amount", "issuer"
  );

  private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  void compareFiveInvoiceParsingPaths() throws Exception {
    Files.createDirectories(OUTPUT_DIR);
    OcrService service = new OcrService(null, null, null, null, objectMapper);
    Method extractFields = OcrService.class.getDeclaredMethod("extractInvoiceFieldsFromText", String.class, String.class);
    extractFields.setAccessible(true);
    Method callOcrService = OcrService.class.getDeclaredMethod("callOcrService", String.class, String.class);
    callOcrService.setAccessible(true);

    ObjectNode report = objectMapper.createObjectNode();
    report.put("sample_dir", SAMPLE_DIR.toString().replace("\\", "/"));
    report.put("output_dir", OUTPUT_DIR.toString().replace("\\", "/"));
    report.put("note", "Diagnostic only. It reuses OcrService text extraction and internal OCR call without writing business tables.");
    ArrayNode samples = report.putArray("samples");

    List<String> sampleNames;
    try (var files = Files.list(SAMPLE_DIR)) {
      sampleNames = files
        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
        .map(path -> path.getFileName().toString())
        .sorted(Comparator.naturalOrder())
        .toList();
    }

    for (String sampleName : sampleNames) {
      Path pdf = SAMPLE_DIR.resolve(sampleName);
      ObjectNode sample = samples.addObject();
      sample.put("file", sampleName);
      sample.put("path", pdf.toString().replace("\\", "/"));

      try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
        String pdfText = new PDFTextStripper().getText(document);
        @SuppressWarnings("unchecked")
        Map<String, String> pdfTextFields = (Map<String, String>) extractFields.invoke(service, pdfText, sampleName);
        addPathResult(sample, "pdf_text", "ok", pdfText, pdfTextFields, null);

        BufferedImage firstPage = new PDFRenderer(document).renderImageWithDPI(0, 200);
        Path imagePath = OUTPUT_DIR.resolve(safeBaseName(sampleName) + ".page1.png");
        ImageIO.write(firstPage, "png", imagePath.toFile());
        sample.put("rendered_page1_png", imagePath.toString().replace("\\", "/"));

        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(firstPage, "png", imageBytes);
        try {
          String ocrJson = (String) callOcrService.invoke(
            service,
            Base64.getEncoder().encodeToString(imageBytes.toByteArray()),
            safeBaseName(sampleName) + ".png");
          JsonNode ocrRoot = objectMapper.readTree(ocrJson);
          addPathResult(
            sample,
            "internal_ocr",
            "ok",
            ocrRoot.path("ocr_text").asText(ocrJson),
            fieldsFromJson(ocrRoot.path("invoice_fields")),
            ocrJson);
        } catch (Exception e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          addPathResult(sample, "internal_ocr", "failed", "", Map.of(), cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
      }
    }

    Files.writeString(
      OUTPUT_DIR.resolve("comparison-report.json"),
      objectMapper.writeValueAsString(report),
      StandardCharsets.UTF_8);
    Files.writeString(
      OUTPUT_DIR.resolve("comparison-report.md"),
      toMarkdown(report),
      StandardCharsets.UTF_8);
    exportExcel(service, report, OUTPUT_DIR.resolve("comparison-report.xlsx"));
  }

  private void addPathResult(ObjectNode sample, String pathName, String status, String text,
                             Map<String, String> fields, String raw) {
    ObjectNode paths = sample.withObject("/paths");
    ObjectNode path = paths.putObject(pathName);
    path.put("status", status);
    path.put("text_length", text == null ? 0 : text.length());
    path.put("text", text == null ? "" : text);
    ObjectNode fieldNode = path.putObject("fields");
    for (String fieldName : FIELD_NAMES) {
      fieldNode.put(fieldName, fields.getOrDefault(fieldName, ""));
    }
    if (raw != null) path.put("raw", raw);
  }

  private Map<String, String> fieldsFromJson(JsonNode node) {
    java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
    if (node != null && node.isObject()) {
      node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue().asText("")));
    }
    return fields;
  }

  private String safeBaseName(String filename) {
    return filename.replaceFirst("\\.pdf$", "").replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
  }

  private String toMarkdown(ObjectNode report) {
    StringBuilder md = new StringBuilder();
    md.append("# OCR Comparison Diagnostic\n\n");
    md.append("- Sample dir: `").append(report.path("sample_dir").asText()).append("`\n");
    md.append("- Output dir: `").append(report.path("output_dir").asText()).append("`\n\n");
    for (JsonNode sample : report.path("samples")) {
      md.append("## ").append(sample.path("file").asText()).append("\n\n");
      md.append("- Rendered image: `").append(sample.path("rendered_page1_png").asText()).append("`\n\n");
      JsonNode paths = sample.path("paths");
      appendPath(md, "pdf_text", paths.path("pdf_text"));
      appendPath(md, "internal_ocr", paths.path("internal_ocr"));
    }
    return md.toString();
  }

  private void appendPath(StringBuilder md, String name, JsonNode path) {
    md.append("### ").append(name).append("\n\n");
    md.append("- Status: `").append(path.path("status").asText()).append("`\n");
    md.append("- Text length: `").append(path.path("text_length").asInt()).append("`\n\n");
    md.append("| Field | Value |\n|---|---|\n");
    JsonNode fields = path.path("fields");
    for (String fieldName : FIELD_NAMES) {
      md.append("| `").append(fieldName).append("` | ")
        .append(fields.path(fieldName).asText("").replace("\n", "<br>"))
        .append(" |\n");
    }
    md.append("\n```text\n");
    String text = path.path("text").asText("");
    md.append(text.length() > 3000 ? text.substring(0, 3000) + "\n...[truncated]" : text);
    md.append("\n```\n\n");
  }

  private void exportExcel(OcrService service, ObjectNode report, Path outputPath) throws Exception {
    try (Workbook workbook = new XSSFWorkbook()) {
      CellStyle headerStyle = headerStyle(workbook);
      CellStyle warningStyle = fillStyle(workbook, IndexedColors.LIGHT_YELLOW);
      CellStyle badStyle = fillStyle(workbook, IndexedColors.ROSE);
      CellStyle okStyle = fillStyle(workbook, IndexedColors.LIGHT_GREEN);

      Sheet summary = workbook.createSheet("总览");
      Sheet detail = workbook.createSheet("变量明细");
      Sheet evidence = workbook.createSheet("区域证据");

      writeHeader(summary, headerStyle, List.of(
        "文件", "是否像标准发票", "区域法命中", "OCR字段命中", "PDF文本层命中",
        "区域法漏字段", "OCR字段漏字段", "区域法发现的疑似错值", "备注"
      ));
      writeHeader(detail, headerStyle, List.of(
        "文件", "变量", "区域法值", "区域来源", "OCR invoice_fields值", "PDF文本层+代码值",
        "状态", "区域证据"
      ));
      writeHeader(evidence, headerStyle, List.of(
        "文件", "购方区", "销方区", "表格区", "合计区", "开票人区"
      ));

      int summaryRow = 1;
      int detailRow = 1;
      int evidenceRow = 1;
      for (JsonNode sample : report.path("samples")) {
        String file = sample.path("file").asText("");
        RegionResult region = parseRegion(service, sample);
        Map<String, String> ocrFields = jsonFields(sample.path("paths").path("internal_ocr").path("fields"));
        Map<String, String> pdfFields = jsonFields(sample.path("paths").path("pdf_text").path("fields"));

        int regionHit = hitCount(region.fields());
        int ocrHit = hitCount(ocrFields);
        int pdfHit = hitCount(pdfFields);
        List<String> regionMissing = missingFields(region.fields());
        List<String> ocrMissing = missingFields(ocrFields);
        List<String> suspicious = suspiciousFields(region, ocrFields, pdfFields);

        Row srow = summary.createRow(summaryRow++);
        int c = 0;
        cell(srow, c++, file);
        cell(srow, c++, regionHit >= 6 ? "是" : "否/弱");
        cell(srow, c++, regionHit + "/" + REGION_FIELD_NAMES.size());
        cell(srow, c++, ocrHit + "/" + REGION_FIELD_NAMES.size());
        cell(srow, c++, pdfHit + "/" + REGION_FIELD_NAMES.size());
        cell(srow, c++, String.join(", ", regionMissing));
        cell(srow, c++, String.join(", ", ocrMissing));
        cell(srow, c++, String.join(", ", suspicious));
        cell(srow, c, summaryNote(region, ocrFields, pdfFields));
        if (!suspicious.isEmpty()) {
          srow.getCell(7).setCellStyle(warningStyle);
        }

        for (String fieldName : REGION_FIELD_NAMES) {
          String regionValue = region.fields().getOrDefault(fieldName, "");
          String ocrValue = ocrFields.getOrDefault(fieldName, "");
          String pdfValue = pdfFields.getOrDefault(fieldName, "");
          String status = fieldStatus(regionValue, ocrValue, pdfValue);
          Row drow = detail.createRow(detailRow++);
          int dc = 0;
          cell(drow, dc++, file);
          cell(drow, dc++, fieldName);
          cell(drow, dc++, regionValue);
          cell(drow, dc++, region.sources().getOrDefault(fieldName, ""));
          cell(drow, dc++, ocrValue);
          cell(drow, dc++, pdfValue);
          Cell statusCell = cell(drow, dc++, status);
          cell(drow, dc, region.evidence().getOrDefault(region.sources().getOrDefault(fieldName, ""), ""));
          if (status.contains("疑似错")) {
            statusCell.setCellStyle(badStyle);
          } else if (status.contains("漏")) {
            statusCell.setCellStyle(warningStyle);
          } else if (status.contains("一致")) {
            statusCell.setCellStyle(okStyle);
          }
        }

        Row erow = evidence.createRow(evidenceRow++);
        int ec = 0;
        cell(erow, ec++, file);
        cell(erow, ec++, region.evidence().getOrDefault("buyer_block", ""));
        cell(erow, ec++, region.evidence().getOrDefault("seller_block", ""));
        cell(erow, ec++, region.evidence().getOrDefault("table_block", ""));
        cell(erow, ec++, region.evidence().getOrDefault("summary_block", ""));
        cell(erow, ec, region.evidence().getOrDefault("issuer_block", ""));
      }

      for (Sheet sheet : List.of(summary, detail, evidence)) {
        sheet.createFreezePane(0, 1);
        int max = sheet == summary ? 9 : sheet == detail ? 8 : 6;
        for (int i = 0; i < max; i++) {
          sheet.autoSizeColumn(i);
          int width = sheet.getColumnWidth(i);
          sheet.setColumnWidth(i, Math.min(Math.max(width, 2800), 18000));
        }
      }

      try (OutputStream out = Files.newOutputStream(outputPath)) {
        workbook.write(out);
      }
    }
  }

  private RegionResult parseRegion(OcrService service, JsonNode sample) throws Exception {
    JsonNode raw = objectMapper.readTree(sample.path("paths").path("internal_ocr").path("raw").asText("{}"));
    List<OcrBox> boxes = new ArrayList<>();
    for (JsonNode item : raw.path("ocr_raw")) {
      boxes.add(new OcrBox(item.path("text").asText(""), item.path("confidence").asDouble(0), item.path("bbox")));
    }

    Map<String, String> fields = new LinkedHashMap<>(service.extractInvoiceFieldsFromOcrRaw(raw, sample.path("file").asText("")));
    Map<String, String> sources = new LinkedHashMap<>();
    Map<String, String> evidence = new LinkedHashMap<>();

    double maxX = boxes.stream().mapToDouble(OcrBox::maxX).max().orElse(0);
    double splitX = maxX / 2;
    double tableY = boxes.stream().filter(b -> b.text().contains("项目名称")).mapToDouble(OcrBox::cy).findFirst().orElse(400);
    double summaryY = boxes.stream().filter(b -> b.text().contains("合计") || "计".equals(b.text())).mapToDouble(OcrBox::cy).filter(y -> y > tableY).findFirst().orElse(760);
    double totalY = boxes.stream().filter(b -> b.text().contains("小写")).mapToDouble(OcrBox::cy).findFirst().orElse(summaryY + 70);
    evidence.put("buyer_block", joinBoxes(boxes, b -> b.cx() < splitX && b.cy() > 180 && b.cy() < tableY));
    evidence.put("seller_block", joinBoxes(boxes, b -> b.cx() >= splitX && b.cy() > 180 && b.cy() < tableY));
    evidence.put("table_block", joinBoxes(boxes, b -> b.cy() >= tableY && b.cy() < summaryY));
    evidence.put("summary_block", joinBoxes(boxes, b -> b.cy() >= summaryY - 60 && b.cy() < totalY + 40));
    evidence.put("issuer_block", joinBoxes(boxes, b -> b.text().contains("开票人") || b.cy() > totalY));
    for (String fieldName : fields.keySet()) {
      sources.put(fieldName, sourceFor(fieldName));
    }

    return new RegionResult(fields, sources, evidence);
  }

  private Map<String, String> jsonFields(JsonNode node) {
    Map<String, String> fields = new LinkedHashMap<>();
    if (node != null && node.isObject()) {
      node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue().asText("")));
    }
    return fields;
  }

  private int hitCount(Map<String, String> fields) {
    int count = 0;
    for (String fieldName : REGION_FIELD_NAMES) {
      if (!fields.getOrDefault(fieldName, "").isBlank()) count++;
    }
    return count;
  }

  private List<String> missingFields(Map<String, String> fields) {
    List<String> missing = new ArrayList<>();
    for (String fieldName : REGION_FIELD_NAMES) {
      if (fields.getOrDefault(fieldName, "").isBlank()) missing.add(fieldName);
    }
    return missing;
  }

  private String sourceFor(String fieldName) {
    return switch (fieldName) {
      case "buyer_name", "buyer_tax_id" -> "buyer_block";
      case "seller_name", "seller_tax_id" -> "seller_block";
      case "amount_before_tax", "tax_amount", "total_amount" -> "summary_or_table_anchor";
      case "issuer" -> "issuer_anchor";
      default -> "all_text_anchor";
    };
  }

  private List<String> suspiciousFields(RegionResult region, Map<String, String> ocrFields, Map<String, String> pdfFields) {
    List<String> suspicious = new ArrayList<>();
    for (String fieldName : REGION_FIELD_NAMES) {
      String regionValue = region.fields().getOrDefault(fieldName, "");
      String ocrValue = ocrFields.getOrDefault(fieldName, "");
      String pdfValue = pdfFields.getOrDefault(fieldName, "");
      if (!ocrValue.isBlank() && !regionValue.isBlank() && !sameValue(ocrValue, regionValue)) {
        suspicious.add(fieldName + ": OCR=" + ocrValue + ", 区域=" + regionValue);
      } else if (!pdfValue.isBlank() && !regionValue.isBlank() && !sameValue(pdfValue, regionValue)) {
        suspicious.add(fieldName + ": PDF=" + pdfValue + ", 区域=" + regionValue);
      }
    }
    return suspicious;
  }

  private String fieldStatus(String regionValue, String ocrValue, String pdfValue) {
    if (regionValue.isBlank() && ocrValue.isBlank() && pdfValue.isBlank()) return "三方都漏";
    if (regionValue.isBlank()) return "区域法漏";
    if (ocrValue.isBlank() && pdfValue.isBlank()) return "区域法独有";
    if (!ocrValue.isBlank() && !sameValue(regionValue, ocrValue)) return "OCR疑似错";
    if (!pdfValue.isBlank() && !sameValue(regionValue, pdfValue)) return "PDF疑似错";
    return "一致/可用";
  }

  private String summaryNote(RegionResult region, Map<String, String> ocrFields, Map<String, String> pdfFields) {
    List<String> notes = new ArrayList<>();
    if (!region.fields().getOrDefault("buyer_name", "").isBlank()
      && !ocrFields.getOrDefault("buyer_name", "").isBlank()
      && !sameValue(region.fields().get("buyer_name"), ocrFields.get("buyer_name"))) {
      notes.add("区域法纠正买方名称");
    }
    if (!region.fields().getOrDefault("amount_before_tax", "").isBlank()
      && ocrFields.getOrDefault("amount_before_tax", "").isBlank()) {
      notes.add("区域法补税前金额");
    }
    if (!region.fields().getOrDefault("tax_amount", "").isBlank()
      && ocrFields.getOrDefault("tax_amount", "").isBlank()) {
      notes.add("区域法补税额");
    }
    if (!region.fields().getOrDefault("issuer", "").isBlank()
      && ocrFields.getOrDefault("issuer", "").isBlank()) {
      notes.add("区域法补开票人");
    }
    if (notes.isEmpty()) notes.add("无明显补强或非标准票面");
    return String.join("；", notes);
  }

  private boolean sameValue(String a, String b) {
    String left = normalizeCompare(a);
    String right = normalizeCompare(b);
    if (left.equals(right)) return true;
    return !left.isBlank() && !right.isBlank() && (left.contains(right) || right.contains(left));
  }

  private String normalizeCompare(String value) {
    if (value == null) return "";
    return value.replace("￥", "").replace("¥", "").replaceAll("\\s+", "").replaceAll("\\.0$", ".00");
  }

  private void put(Map<String, String> fields, Map<String, String> sources, String field, String value, String source) {
    if (value != null && !value.isBlank()) {
      fields.put(field, value);
      sources.put(field, source);
    }
  }

  private String allText(List<OcrBox> boxes) {
    StringBuilder text = new StringBuilder();
    for (OcrBox box : boxes) text.append(box.text()).append('\n');
    return text.toString();
  }

  private String firstBoxValue(List<OcrBox> boxes, java.util.function.Predicate<OcrBox> predicate,
                               java.util.function.Function<String, String> transform) {
    return boxes.stream()
      .filter(predicate)
      .sorted(Comparator.comparingDouble(OcrBox::cy).thenComparingDouble(OcrBox::cx))
      .map(box -> transform.apply(box.text()))
      .filter(value -> value != null && !value.isBlank())
      .findFirst()
      .orElse("");
  }

  private String joinBoxes(List<OcrBox> boxes, java.util.function.Predicate<OcrBox> predicate) {
    String joined = String.join(" | ", boxes.stream()
      .filter(predicate)
      .sorted(Comparator.comparingDouble(OcrBox::cy).thenComparingDouble(OcrBox::cx))
      .map(OcrBox::text)
      .toList());
    return joined.length() > 500 ? joined.substring(0, 500) + "..." : joined;
  }

  private boolean hasTaxId(String text) {
    return Pattern.compile("[0-9A-Z]{15,20}").matcher(text).find();
  }

  private String firstMatch(String text, String regex) {
    Matcher matcher = Pattern.compile(regex).matcher(text == null ? "" : text);
    return matcher.find() ? matcher.group(1) : "";
  }

  private String afterLabel(String text) {
    if (text == null) return "";
    int colon = Math.max(text.indexOf('：'), text.indexOf(':'));
    if (colon >= 0 && colon + 1 < text.length()) return text.substring(colon + 1).trim();
    return "";
  }

  private String money(String text) {
    return firstMatch(text, "[¥￥]?\\s*(\\d+\\.\\d{1,2})");
  }

  private Cell cell(Row row, int column, String value) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value == null ? "" : value);
    return cell;
  }

  private void writeHeader(Sheet sheet, CellStyle style, List<String> headers) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.size(); i++) {
      Cell cell = cell(row, i, headers.get(i));
      cell.setCellStyle(style);
    }
  }

  private CellStyle headerStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private CellStyle fillStyle(Workbook workbook, IndexedColors color) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(color.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private record RegionResult(Map<String, String> fields, Map<String, String> sources, Map<String, String> evidence) {
  }

  private record OcrBox(String text, double confidence, JsonNode bbox) {
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
}
