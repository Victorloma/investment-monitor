package com.victorloma.investmentmonitor.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePortfolioEntryRequest(
    @NotBlank @Size(max = 10) String ticker,
    @Size(max = 255) String companyName,
    @DecimalMin(value = "0.0", inclusive = false) BigDecimal alertThreshold,
    boolean monitored) {}
