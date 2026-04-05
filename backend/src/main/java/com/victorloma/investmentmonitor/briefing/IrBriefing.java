package com.victorloma.investmentmonitor.briefing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ir_briefings")
public class IrBriefing {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "portfolio_entry_id", nullable = false)
  private UUID portfolioEntryId;

  @Column(name = "briefing_json", nullable = false, columnDefinition = "TEXT")
  private String briefingJson;

  @Column(name = "fetched_at", nullable = false, updatable = false)
  private Instant fetchedAt;

  @PrePersist
  void prePersist() {
    fetchedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getPortfolioEntryId() {
    return portfolioEntryId;
  }

  public void setPortfolioEntryId(UUID portfolioEntryId) {
    this.portfolioEntryId = portfolioEntryId;
  }

  public String getBriefingJson() {
    return briefingJson;
  }

  public void setBriefingJson(String briefingJson) {
    this.briefingJson = briefingJson;
  }

  public Instant getFetchedAt() {
    return fetchedAt;
  }
}
