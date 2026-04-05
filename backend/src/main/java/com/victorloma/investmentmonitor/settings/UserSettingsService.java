package com.victorloma.investmentmonitor.settings;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  static final String DEFAULT_PROVIDER_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
  static final String DEFAULT_MODEL_ID = "gemini-2.0-flash-lite";

  private final UserSettingsRepository userSettingsRepository;
  private final SecretKey secretKey;

  public UserSettingsService(
      UserSettingsRepository userSettingsRepository,
      @Value("${app.encryption.secret}") String secret) {
    this.userSettingsRepository = userSettingsRepository;
    byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  @Transactional(readOnly = true)
  public boolean hasLlmApiKey(UUID userId) {
    return userSettingsRepository.findByUserId(userId)
        .map(s -> s.getLlmApiKeyEncrypted() != null && !s.getLlmApiKeyEncrypted().isBlank())
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public Optional<LlmConfig> getLlmConfig(UUID userId) {
    return userSettingsRepository.findByUserId(userId)
        .filter(s -> s.getLlmApiKeyEncrypted() != null && !s.getLlmApiKeyEncrypted().isBlank())
        .map(s -> new LlmConfig(
            decrypt(s.getLlmApiKeyEncrypted()),
            coalesce(s.getLlmProviderUrl(), DEFAULT_PROVIDER_URL),
            coalesce(s.getLlmModelId(), DEFAULT_MODEL_ID)));
  }

  @Transactional
  public void saveSettings(UUID userId, String plainApiKey, String providerUrl, String modelId) {
    UserSettings settings = userSettingsRepository.findByUserId(userId)
        .orElseGet(() -> {
          UserSettings s = new UserSettings();
          s.setUserId(userId);
          return s;
        });

    if (plainApiKey != null && !plainApiKey.isBlank()) {
      settings.setLlmApiKeyEncrypted(encrypt(plainApiKey));
    }
    settings.setLlmProviderUrl(blankToNull(providerUrl));
    settings.setLlmModelId(blankToNull(modelId));
    userSettingsRepository.save(settings);
  }

  private String coalesce(String value, String fallback) {
    return (value != null && !value.isBlank()) ? value : fallback;
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }

  private String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to encrypt value", e);
    }
  }

  private String decrypt(String encoded) {
    try {
      byte[] combined = Base64.getDecoder().decode(encoded);
      byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
      byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decrypt value", e);
    }
  }

  public record LlmConfig(String apiKey, String providerUrl, String modelId) {}
}
