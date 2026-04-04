package com.victorloma.investmentmonitor.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioEntryResponse(
    UUID id,
    String ticker,
    String companyName,
    BigDecimal alertThreshold,
    boolean monitored,
    Instant addedAt) {}
