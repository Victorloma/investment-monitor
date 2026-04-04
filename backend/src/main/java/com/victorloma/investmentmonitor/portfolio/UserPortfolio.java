package com.victorloma.investmentmonitor.portfolio;

import com.victorloma.investmentmonitor.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_portfolios")
public class UserPortfolio {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "alert_threshold", precision = 10, scale = 2)
  private BigDecimal alertThreshold;

  @Column(name = "is_monitored", nullable = false)
  private boolean monitored = true;

  @Column(name = "added_at", nullable = false, updatable = false)
  private Instant addedAt;

  @PrePersist
  void prePersist() {
    addedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public BigDecimal getAlertThreshold() {
    return alertThreshold;
  }

  public void setAlertThreshold(BigDecimal alertThreshold) {
    this.alertThreshold = alertThreshold;
  }

  public boolean isMonitored() {
    return monitored;
  }

  public void setMonitored(boolean monitored) {
    this.monitored = monitored;
  }

  public Instant getAddedAt() {
    return addedAt;
  }
}
