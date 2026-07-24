package tech.wetech.admin3.sys.service.ocr;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class OcrSecretCipher {

  private static final String ENV_KEY = "ADMIN3_OCR_SECRET_KEY";
  private static final String DEV_KEY = "admin3-ocr-dev-secret-change-me";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private final SecureRandom secureRandom = new SecureRandom();

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) return null;
    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to encrypt OCR API key", e);
    }
  }

  public String decrypt(String cipherText) {
    if (cipherText == null || cipherText.isBlank()) return null;
    try {
      String[] parts = cipherText.split(":", 2);
      if (parts.length != 2) throw new IllegalArgumentException("Invalid OCR API key cipher text");
      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] encrypted = Base64.getDecoder().decode(parts[1]);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decrypt OCR API key", e);
    }
  }

  private SecretKeySpec keySpec() throws Exception {
    String secret = System.getenv().getOrDefault(ENV_KEY, DEV_KEY);
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
    return new SecretKeySpec(digest, "AES");
  }
}
