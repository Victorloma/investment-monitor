package com.victorloma.investmentmonitor.portfolio.dto;

import java.util.List;
import java.util.UUID;

public record PortfolioCrawlPreviewResponse(
    UUID portfolioEntryId,
    String ticker,
    String companyName,
    String irUrl,
    int candidateCount,
    List<CrawlLinkCandidate> candidates) {

  public record CrawlLinkCandidate(String url, String title) {}
}
