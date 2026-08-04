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
}
