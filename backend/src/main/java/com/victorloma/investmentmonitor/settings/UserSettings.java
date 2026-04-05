package com.victorloma.investmentmonitor.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings {

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "llm_api_key_encrypted")
  private String llmApiKeyEncrypted;

  @Column(name = "llm_provider_url", length = 500)
  private String llmProviderUrl;

  @Column(name = "llm_model_id", length = 200)
  private String llmModelId;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getLlmApiKeyEncrypted() {
    return llmApiKeyEncrypted;
  }

  public void setLlmApiKeyEncrypted(String llmApiKeyEncrypted) {
    this.llmApiKeyEncrypted = llmApiKeyEncrypted;
  }

  public String getLlmProviderUrl() {
    return llmProviderUrl;
  }

  public void setLlmProviderUrl(String llmProviderUrl) {
    this.llmProviderUrl = llmProviderUrl;
  }

  public String getLlmModelId() {
    return llmModelId;
  }

  public void setLlmModelId(String llmModelId) {
    this.llmModelId = llmModelId;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
