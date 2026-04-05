package com.victorloma.investmentmonitor.portfolio;

import com.victorloma.investmentmonitor.portfolio.dto.PortfolioCrawlPreviewResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IrCrawlService {

  private static final Pattern ANCHOR_PATTERN =
      Pattern.compile("<a\\b[^>]*href\\s*=\\s*(['\"])(.*?)\\1[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
  private static final int MAX_CANDIDATES = 25;

  private final UserPortfolioRepository userPortfolioRepository;
  private final HttpClient httpClient;

  public IrCrawlService(UserPortfolioRepository userPortfolioRepository) {
    this.userPortfolioRepository = userPortfolioRepository;
    this.httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  @Transactional(readOnly = true)
  public PortfolioCrawlPreviewResponse previewPortfolioEntryIrLinks(UUID userId, UUID portfolioEntryId) {
    UserPortfolio portfolioEntry =
        userPortfolioRepository
            .findByIdAndUserId(portfolioEntryId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio entry not found"));

    Company company = portfolioEntry.getCompany();
    if (company.getIrUrl() == null || company.getIrUrl().isBlank()) {
      throw new IllegalArgumentException("No IR URL configured for this portfolio entry");
    }

    URI irUri = toUri(company.getIrUrl());
    String html = fetchHtml(irUri);
    List<PortfolioCrawlPreviewResponse.CrawlLinkCandidate> candidates = extractCandidates(irUri, html);

    return new PortfolioCrawlPreviewResponse(
        portfolioEntry.getId(),
        company.getTicker(),
        company.getName(),
        company.getIrUrl(),
        candidates.size(),
        candidates);
  }

  private String fetchHtml(URI irUri) {
    HttpRequest request =
        HttpRequest.newBuilder(irUri)
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "InvestmentMonitorBot/0.1")
            .GET()
            .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalArgumentException(
            "Failed to fetch IR page: HTTP status " + response.statusCode());
      }
      return response.body();
    } catch (IOException | InterruptedException exception) {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalArgumentException("Failed to fetch IR page");
    }
  }

  private List<PortfolioCrawlPreviewResponse.CrawlLinkCandidate> extractCandidates(URI irUri, String html) {
    Matcher matcher = ANCHOR_PATTERN.matcher(html);
    Map<String, PortfolioCrawlPreviewResponse.CrawlLinkCandidate> candidatesByUrl = new LinkedHashMap<>();

    while (matcher.find()) {
      String href = matcher.group(2);
      String rawText = matcher.group(3);
      URI resolvedUri = resolveSameDomainUri(irUri, href);
      if (resolvedUri == null) {
        continue;
      }

      String title = normalizeAnchorText(rawText);
      if (!looksRelevant(resolvedUri, title)) {
        continue;
      }

      String normalizedUrl = resolvedUri.toString();
      candidatesByUrl.putIfAbsent(
          normalizedUrl,
          new PortfolioCrawlPreviewResponse.CrawlLinkCandidate(normalizedUrl, title));
    }

    return candidatesByUrl.values().stream()
        .sorted(Comparator.comparing(PortfolioCrawlPreviewResponse.CrawlLinkCandidate::url))
        .limit(MAX_CANDIDATES)
        .toList();
  }

  private URI resolveSameDomainUri(URI baseUri, String href) {
    if (href == null || href.isBlank()) {
      return null;
    }

    String trimmedHref = href.trim();
    String lowercaseHref = trimmedHref.toLowerCase(Locale.ROOT);
    if (lowercaseHref.startsWith("javascript:")
        || lowercaseHref.startsWith("mailto:")
        || lowercaseHref.startsWith("tel:")) {
      return null;
    }

    try {
      URI resolvedUri = baseUri.resolve(trimmedHref).normalize();
      String scheme = resolvedUri.getScheme();
      String host = resolvedUri.getHost();
      if (scheme == null || host == null) {
        return null;
      }
      if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
        return null;
      }
      if (!host.equalsIgnoreCase(baseUri.getHost())) {
        return null;
      }
      return stripFragment(resolvedUri);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private URI stripFragment(URI uri) {
    try {
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          uri.getHost(),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          null);
    } catch (URISyntaxException exception) {
      return uri;
    }
  }

  private boolean looksRelevant(URI uri, String title) {
    String path = (uri.getPath() == null ? "" : uri.getPath()).toLowerCase(Locale.ROOT);
    String haystack = (path + " " + (title == null ? "" : title)).toLowerCase(Locale.ROOT);
    return haystack.contains("invest")
        || haystack.contains("ir")
        || haystack.contains("press")
        || haystack.contains("news")
        || haystack.contains("release")
        || haystack.contains("earn")
        || haystack.contains("filing")
        || haystack.contains("financial")
        || haystack.contains("result")
        || haystack.contains("presentation")
        || haystack.contains("event")
        || haystack.contains("webcast")
        || path.endsWith(".pdf");
  }

  private String normalizeAnchorText(String rawText) {
    if (rawText == null || rawText.isBlank()) {
      return "";
    }
    String withoutTags = TAG_PATTERN.matcher(rawText).replaceAll(" ");
    return withoutTags.replaceAll("\\s+", " ").trim();
  }

  private URI toUri(String value) {
    try {
      return new URI(value);
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("IR URL is not valid for crawling");
    }
  }
}
