package com.victorloma.investmentmonitor.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioEntryResponse(
    UUID id,
    String ticker,
    String companyName,
    String irUrl,
    BigDecimal alertThreshold,
    boolean monitored,
    Instant addedAt) {}
