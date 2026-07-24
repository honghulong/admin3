package tech.wetech.admin3.sys.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.wetech.admin3.sys.model.OcrEngineConfig;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleOcrProviderTest {

  @Test
  void normalizeResponseExtractsJsonMessageContent() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    OpenAiCompatibleOcrProvider provider = new OpenAiCompatibleOcrProvider(objectMapper, new OcrSecretCipher());
    String response = """
      {
        "choices": [
          {
            "message": {
              "content": "```json\\n{\\\"invoice_no\\\":\\\"12345678\\\",\\\"total_amount\\\":\\\"88.60\\\"}\\n```"
            }
          }
        ]
      }
      """;

    String normalized = provider.normalizeResponse(response, "QWEN");

    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/invoice_no").asText()).isEqualTo("12345678");
    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/total_amount").asText()).isEqualTo("88.60");
    assertThat(objectMapper.readTree(normalized).at("/source_type").asText()).isEqualTo("QWEN");
  }

  @Test
  void normalizeResponseMapsProviderAliasesAndNullText() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    OpenAiCompatibleOcrProvider provider = new OpenAiCompatibleOcrProvider(objectMapper, new OcrSecretCipher());
    String response = """
      {
        "choices": [
          {
            "message": {
              "content": "{\\\"invoice_number\\\":\\\"72551412\\\",\\\"invoice_date\\\":\\\"2023年01月21日\\\",\\\"seller_name\\\":\\\"null\\\",\\\"buyer_bank_account\\\":\\\"准格尔旗建行兴隆东街支行 15001886638052500568\\\",\\\"seller_address_tel\\\":\\\"内蒙古自治区呼和浩特市回民区金海国际五金城B5-09 13654895658\\\"}"
            }
          }
        ]
      }
      """;

    String normalized = provider.normalizeResponse(response, "QWEN");

    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/invoice_no").asText()).isEqualTo("72551412");
    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/date").asText()).isEqualTo("2023年01月21日");
    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/seller_name").asText()).isEmpty();
    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/buyer_bank_info").asText()).contains("准格尔旗建行");
    assertThat(objectMapper.readTree(normalized).at("/invoice_fields/seller_address_phone").asText()).contains("呼和浩特市");
  }

  @Test
  void kimiRequestDisablesThinkingAndUsesShortTimeout() {
    ObjectMapper objectMapper = new ObjectMapper();
    OpenAiCompatibleOcrProvider provider = new OpenAiCompatibleOcrProvider(objectMapper, new OcrSecretCipher());
    OcrEngineConfig config = new OcrEngineConfig();
    config.setProviderType("KIMI");
    config.setModelName("kimi-k2.6");
    OcrRecognitionRequest request = new OcrRecognitionRequest("abc", "invoice.png", null);

    var body = provider.buildRequestBody(config, request);

    assertThat(OpenAiCompatibleOcrProvider.REQUEST_TIMEOUT.getSeconds()).isEqualTo(45);
    assertThat(body.at("/thinking/type").asText()).isEqualTo("disabled");
    assertThat(body.at("/messages/0/content/0/image_url/url").asText()).startsWith("data:image/png;base64,abc");
  }
}
