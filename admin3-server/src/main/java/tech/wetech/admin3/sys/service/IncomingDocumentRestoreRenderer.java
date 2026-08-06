package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class IncomingDocumentRestoreRenderer {

  private final ObjectMapper objectMapper;

  IncomingDocumentRestoreRenderer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  String render(String rawJson) throws Exception {
    JsonNode root = objectMapper.readTree(rawJson);
    JsonNode page = root.path("pages").isArray() && !root.path("pages").isEmpty() ? root.path("pages").get(0) : root;
    int width = page.path("width").asInt(1000);
    int height = page.path("height").asInt(1400);
    JsonNode restored = page.path("restore").path("region_restore");
    if (!restored.path("regions").isArray()) {
      throw new IllegalArgumentException("OCR JSON does not contain restore.region_restore.regions");
    }

    List<OcrItem> pageLines = overallOcrLines(page);
    List<Rect> regionRects = new ArrayList<>();
    StringBuilder parts = new StringBuilder();
    for (JsonNode region : iterable(restored.path("regions"))) {
      Rect rect = rect(region.path("rect"));
      if (rect == null) continue;
      regionRects.add(rect);
      String kind = attr(region.path("kind").asText("region"));
      parts.append("<div class=\"paint-region ").append(kind).append("\" style=\"")
        .append(cssRect(rect, width, height)).append("\"><span class=\"paint-region-label\">R")
        .append(region.path("index").asInt()).append("</span></div>");
    }
    List<JsonNode> supplementalCells = supplementalTableCells(page, regionRects);
    int cellIndex = 0;
    for (JsonNode cell : supplementalCells) {
      Rect rect = rect(cell.path("rect"));
      if (rect == null) continue;
      parts.append("<div class=\"paint-region table-cell\" style=\"")
        .append(cssRect(rect, width, height)).append("\"><span class=\"paint-region-label\">C")
        .append(cellIndex++).append("</span></div>");
    }
    for (JsonNode region : iterable(restored.path("regions"))) {
      for (JsonNode item : iterable(region.path("items"))) {
        appendPaintText(parts, item.path("text").asText(""), rect(item.path("rect")), width, height);
      }
    }
    for (JsonNode cell : supplementalCells) {
      for (JsonNode item : iterable(cell.path("items"))) {
        appendPaintText(parts, item.path("text").asText(""), rect(item.path("rect")), width, height);
      }
    }
    for (JsonNode block : iterable(restored.path("page_blocks"))) {
      Rect blockRect = rect(block.path("rect"));
      String blockText = block.path("text").asText("").trim();
      if (blockRect == null || blockText.isBlank()) continue;
      List<OcrItem> innerLines = linesInsideBlock(blockText, blockRect, pageLines);
      if (!innerLines.isEmpty()) {
        for (OcrItem line : innerLines) appendPaintBlock(parts, line.text(), line.rect(), width, height);
      } else {
        appendPaintBlock(parts, blockText, blockRect, width, height);
      }
    }

    return """
      <!doctype html>
      <html lang="zh-CN">
      <head>
      <meta charset="utf-8">
      <title>incoming document restore</title>
      <style>
      body { margin: 0; padding: 24px; background: #e5e7eb; font-family: Arial, "Microsoft YaHei", sans-serif; }
      .paint-page { position: relative; max-width: 1180px; margin: 0 auto; background: white; aspect-ratio: %d/%d; box-shadow: 0 2px 14px rgba(15,23,42,.18); overflow: hidden; }
      .paint-region { position: absolute; box-sizing: border-box; border: 2px solid #991b1b; background: transparent; }
      .paint-region.detail { border-color: #1d4ed8; }
      .paint-region.remark { border-color: #be123c; }
      .paint-region.table-cell { border-color: #16a34a; border-style: dashed; }
      .paint-region-label { position: absolute; left: 2px; top: 1px; color: #991b1b; font-size: 10px; font-weight: 700; }
      .paint-region.detail .paint-region-label { color: #1d4ed8; }
      .paint-region.table-cell .paint-region-label { color: #15803d; }
      .paint-text { position: absolute; box-sizing: border-box; color: #111827; font-size: 14px; line-height: 1.05; white-space: nowrap; overflow: visible; }
      .paint-text.vertical { writing-mode: vertical-rl; text-orientation: upright; display: flex; align-items: center; justify-content: center; white-space: normal; line-height: 1.08; }
      .paint-block { position: absolute; box-sizing: border-box; color: #7f1d1d; font-size: 15px; line-height: 1.1; white-space: pre-wrap; overflow: visible; }
      </style>
      </head>
      <body><section class="paint-page">%s</section></body>
      </html>
      """.formatted(width, height, parts);
  }

  private void appendPaintText(StringBuilder parts, String text, Rect rect, int width, int height) {
    if (rect == null || text == null || text.isBlank()) return;
    String cssClass = looksVertical(text, rect) ? "paint-text vertical" : "paint-text";
    parts.append("<div class=\"").append(cssClass).append("\" style=\"")
      .append(cssRect(rect, width, height)).append("\">")
      .append(escape(text)).append("</div>");
  }

  private void appendPaintBlock(StringBuilder parts, String text, Rect rect, int width, int height) {
    if (rect == null || text == null || text.isBlank()) return;
    parts.append("<div class=\"paint-block\" style=\"")
      .append(cssRect(rect, width, height)).append("\">")
      .append(escape(text)).append("</div>");
  }

  private List<OcrItem> overallOcrLines(JsonNode page) {
    JsonNode rawResults = page.path("structure").path("raw_results");
    if (!rawResults.isArray() || rawResults.isEmpty()) return List.of();
    JsonNode overall = rawResults.get(0).path("res").path("overall_ocr_res");
    JsonNode texts = overall.path("rec_texts");
    JsonNode boxes = overall.has("rec_boxes") ? overall.path("rec_boxes") : overall.path("rec_polys");
    if (!texts.isArray() || !boxes.isArray()) return List.of();
    List<OcrItem> lines = new ArrayList<>();
    int count = Math.min(texts.size(), boxes.size());
    for (int i = 0; i < count; i++) {
      Rect rect = rect(boxes.get(i));
      String text = texts.get(i).asText("").trim();
      if (rect != null && !text.isBlank()) lines.add(new OcrItem(text, rect));
    }
    return lines;
  }

  private List<JsonNode> supplementalTableCells(JsonNode page, List<Rect> regionRects) {
    JsonNode tables = page.path("restore").path("cell_mapping").path("tables");
    if (!tables.isArray()) return List.of();
    List<JsonNode> cells = new ArrayList<>();
    for (JsonNode table : iterable(tables)) {
      for (JsonNode cell : iterable(table.path("cells"))) {
        Rect rect = rect(cell.path("rect"));
        if (rect != null && !isCoveredByRegions(rect, regionRects)) cells.add(cell);
      }
    }
    return cells;
  }

  private boolean isCoveredByRegions(Rect rect, List<Rect> regionRects) {
    return regionRects.stream().anyMatch(region -> overlapRatio(rect, region) > 0.72 || centerInside(rect, region));
  }

  private List<OcrItem> linesInsideBlock(String blockText, Rect blockRect, List<OcrItem> lines) {
    String compactBlock = compact(blockText);
    return lines.stream()
      .filter(line -> compactBlock.contains(compact(line.text())))
      .filter(line -> overlapRatio(line.rect(), blockRect) > 0 || centerInside(line.rect(), blockRect))
      .sorted(Comparator.comparingDouble((OcrItem item) -> item.rect().y1()).thenComparingDouble(item -> item.rect().x1()))
      .toList();
  }

  private static Iterable<JsonNode> iterable(JsonNode node) {
    return node::elements;
  }

  private static Rect rect(JsonNode node) {
    if (!node.isArray() || node.size() < 4) return null;
    if (node.get(0).isArray()) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      for (JsonNode point : iterable(node)) {
        if (!point.isArray() || point.size() < 2) continue;
        double x = point.get(0).asDouble();
        double y = point.get(1).asDouble();
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
      return minX == Double.MAX_VALUE ? null : new Rect(minX, minY, maxX, maxY);
    }
    return new Rect(node.get(0).asDouble(), node.get(1).asDouble(), node.get(2).asDouble(), node.get(3).asDouble());
  }

  private static String cssRect(Rect rect, int width, int height) {
    return "left:%.4f%%;top:%.4f%%;width:%.4f%%;height:%.4f%%;"
      .formatted(100 * rect.x1() / width, 100 * rect.y1() / height, 100 * rect.width() / width, 100 * rect.height() / height);
  }

  private static boolean looksVertical(String text, Rect rect) {
    String value = text.trim();
    if (value.length() < 2 || value.length() > 8) return false;
    if (rect.height() / Math.max(1, rect.width()) < 1.8) return false;
    long cjk = value.chars().filter(ch -> ch >= 0x4e00 && ch <= 0x9fff).count();
    return cjk >= Math.ceil(value.length() * 0.75);
  }

  private static double overlapRatio(Rect a, Rect b) {
    double x1 = Math.max(a.x1(), b.x1());
    double y1 = Math.max(a.y1(), b.y1());
    double x2 = Math.min(a.x2(), b.x2());
    double y2 = Math.min(a.y2(), b.y2());
    if (x2 <= x1 || y2 <= y1) return 0;
    return ((x2 - x1) * (y2 - y1)) / Math.max(1, a.width() * a.height());
  }

  private static boolean centerInside(Rect a, Rect b) {
    double cx = (a.x1() + a.x2()) / 2;
    double cy = (a.y1() + a.y2()) / 2;
    return b.x1() <= cx && cx <= b.x2() && b.y1() <= cy && cy <= b.y2();
  }

  private static String compact(String text) {
    return text == null ? "" : text.replaceAll("\\s+", "");
  }

  private static String attr(String value) {
    return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "");
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private record Rect(double x1, double y1, double x2, double y2) {
    double width() { return Math.max(1, x2 - x1); }
    double height() { return Math.max(1, y2 - y1); }
  }

  private record OcrItem(String text, Rect rect) {}
}
