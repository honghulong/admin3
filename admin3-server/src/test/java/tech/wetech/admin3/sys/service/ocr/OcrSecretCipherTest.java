package tech.wetech.admin3.sys.service.ocr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcrSecretCipherTest {

  @Test
  void encryptsAndDecryptsApiKey() {
    OcrSecretCipher cipher = new OcrSecretCipher();

    String encrypted = cipher.encrypt("sk-test");

    assertThat(encrypted).isNotEqualTo("sk-test");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("sk-test");
  }
}
