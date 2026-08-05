package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncomingDocumentOcrMapperTest {

  private final IncomingDocumentOcrMapper mapper = new IncomingDocumentOcrMapper(new ObjectMapper());

  @Test
  void extractBusinessFieldsSkipsSystemResponseFields() {
    String raw = """
      {
        "code": 0,
        "message": "ok",
        "data": {
          "title": "关于召开会议的通知",
          "doc_no": "粤办函〔2026〕1号",
          "result": {
            "handler": "张三",
            "status": "ignored"
          }
        }
      }
      """;

    Map<String, String> fields = mapper.extractBusinessFields(raw);

    assertThat(fields)
      .containsEntry("title", "关于召开会议的通知")
      .containsEntry("doc_no", "粤办函〔2026〕1号")
      .containsEntry("handler", "张三")
      .doesNotContainKeys("code", "message", "status");
  }

  @Test
  void extractBusinessFieldsFlattensNestedArrays() {
    String raw = """
      {
        "success": true,
        "data": {
          "items": [
            {"name": "密级", "value": "秘密"}
          ]
        }
      }
      """;

    assertThat(mapper.extractBusinessFields(raw))
      .containsEntry("items.0.name", "密级")
      .containsEntry("items.0.value", "秘密");
  }
  @Test
  void extractBusinessFieldsPrefersGpuOcrPageLines() {
    String raw = """
      {
        "engine": "paddleocr",
        "input": {"filename": "scan.png"},
        "model": {"det": "PP-OCRv5_server_det"},
        "pages": [
          {
            "page_no": 1,
            "lines": [
              {"text": "\\u6536\\u6587\\u7f16\\u53f7", "confidence": 0.999704, "x1": 29, "y1": 277, "x2": 163, "y2": 320},
              {"text": "LW20260804-001", "confidence": 0.997856, "x1": 315, "y1": 287, "x2": 529, "y2": 315},
              {"text": "\\u6587\\u4ef6\\u6807\\u9898", "confidence": 0.999350, "x1": 27, "y1": 417, "x2": 161, "y2": 456}
            ]
          }
        ]
      }
      """;

    Map<String, String> fields = mapper.extractBusinessFields(raw);

    assertThat(fields)
      .containsEntry("line.001", "\u6536\u6587\u7f16\u53f7 | p1 | conf 1.000 | box 29,277,163,320")
      .containsEntry("line.002", "LW20260804-001 | p1 | conf 0.998 | box 315,287,529,315")
      .containsEntry("line.003", "\u6587\u4ef6\u6807\u9898 | p1 | conf 0.999 | box 27,417,161,456")
      .containsEntry("plain_text", "\u6536\u6587\u7f16\u53f7\nLW20260804-001\n\u6587\u4ef6\u6807\u9898")
      .doesNotContainKeys("engine", "input.filename", "model.det");
  }
}
