package com.victorloma.investmentmonitor.briefing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.victorloma.investmentmonitor.portfolio.UserPortfolio;
import com.victorloma.investmentmonitor.portfolio.UserPortfolioRepository;
import com.victorloma.investmentmonitor.settings.UserSettingsService;
import com.victorloma.investmentmonitor.settings.UserSettingsService.LlmConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IrBriefingService {

  private static final Logger log = LoggerFactory.getLogger(IrBriefingService.class);

  private static final String SYSTEM_PROMPT = """
      You are a senior investor relations analyst. Summarize the most recent \
      investor-relevant updates for the given company using your knowledge.

      Cover:
      - Latest earnings releases and financial results (revenue, EPS, guidance)
      - Recent SEC filings (8-K, 10-Q, 10-K) with key highlights
      - Material press releases (M&A, leadership changes, product launches)
      - Upcoming earnings calls, investor days, or webcasts

      Return ONLY a valid JSON object in exactly this structure (no markdown, no extra text):
      {
        "asOf": "<ISO 8601 date of your knowledge cutoff or today>",
        "analystSummary": "<2-3 sentence high-level synthesis for an investor>",
        "items": [
          {
            "type": "<EARNINGS | FILING | PRESS_RELEASE | EVENT | OTHER>",
            "title": "<concise title>",
            "date": "<ISO 8601 date or null>",
            "summary": "<2-3 factual sentences>",
            "url": "<source URL if known, otherwise null>"
          }
        ]
      }

      Facts only. No speculation. Maximum 8 items. \
      If you have no reliable information, return an empty items array with an honest analystSummary.
      """;

  private final UserPortfolioRepository userPortfolioRepository;
  private final IrBriefingRepository irBriefingRepository;
  private final UserSettingsService userSettingsService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public IrBriefingService(
      UserPortfolioRepository userPortfolioRepository,
      IrBriefingRepository irBriefingRepository,
      UserSettingsService userSettingsService,
      ObjectMapper objectMapper) {
    this.userPortfolioRepository = userPortfolioRepository;
    this.irBriefingRepository = irBriefingRepository;
    this.userSettingsService = userSettingsService;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  @Transactional(readOnly = true)
  public Optional<IrBriefingResponse> getLatestBriefing(UUID userId, UUID portfolioEntryId) {
    verifyOwnership(userId, portfolioEntryId);
    return irBriefingRepository
        .findTopByPortfolioEntryIdOrderByFetchedAtDesc(portfolioEntryId)
        .map(this::toResponse);
  }

  @Transactional
  public IrBriefingResponse fetchAndSaveBriefing(UUID userId, UUID portfolioEntryId) {
    UserPortfolio entry = verifyOwnership(userId, portfolioEntryId);
    LlmConfig config = requireLlmConfig(userId);
    return fetchAndSaveBriefingWithConfig(entry, config);
  }

  @Transactional
  public List<IrBriefingResponse> fetchAndSaveAllBriefings(UUID userId) {
    LlmConfig config = requireLlmConfig(userId);
    return userPortfolioRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
        .map(entry -> fetchAndSaveBriefingWithConfig(entry, config))
        .toList();
  }

  private IrBriefingResponse fetchAndSaveBriefingWithConfig(UserPortfolio entry, LlmConfig config) {
    String userMessage = buildUserMessage(
        entry.getCompany().getName(),
        entry.getCompany().getTicker(),
        entry.getCompany().getIrUrl());

    String briefingJson = stripMarkdownFences(callLlmApi(config, userMessage));

    IrBriefing briefing = new IrBriefing();
    briefing.setPortfolioEntryId(entry.getId());
    briefing.setBriefingJson(briefingJson);
    irBriefingRepository.save(briefing);

    return toResponse(briefing);
  }

  private String buildUserMessage(String companyName, String ticker, String irUrl) {
    StringBuilder sb = new StringBuilder();
    sb.append("Find the latest investor relations updates for ")
        .append(companyName).append(" (ticker: ").append(ticker).append(").");
    if (irUrl != null && !irUrl.isBlank()) {
      sb.append(" Their IR page is: ").append(irUrl).append(".");
    }
    sb.append(" Focus on news from the last 90 days.");
    return sb.toString();
  }

  private String callLlmApi(LlmConfig config, String userMessage) {
    try {
      List<Map<String, Object>> tools = webSearchTools(config.providerUrl());

      Map<String, Object> bodyMutable = new java.util.LinkedHashMap<>();
      bodyMutable.put("model", config.modelId());
      bodyMutable.put("messages", List.of(
          Map.of("role", "system", "content", SYSTEM_PROMPT),
          Map.of("role", "user", "content", userMessage)
      ));
      if (!tools.isEmpty()) {
        bodyMutable.put("tools", tools);
      }
      bodyMutable.put("max_tokens", 1024);
      bodyMutable.put("temperature", 1.0);
      bodyMutable.put("top_p", 1.0);
      bodyMutable.put("stream", false);
      Map<String, Object> body = java.util.Collections.unmodifiableMap(bodyMutable);

      String requestBody = objectMapper.writeValueAsString(body);
      String base = config.providerUrl().strip().replaceAll("/+$", "");
      String apiUrl = base.endsWith("/chat/completions")
          ? base
          : base + "/chat/completions";

      log.info("Calling LLM API: POST {} model={}", apiUrl, config.modelId());

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .timeout(Duration.ofSeconds(300))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("Authorization", "Bearer " + config.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      log.info("LLM API response: HTTP {}", response.statusCode());

      if (response.statusCode() == 401) {
        log.warn("LLM API 401 body: {}", response.body());
        throw new IllegalArgumentException("LLM API key is invalid or expired.");
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("LLM API error body: {}", response.body());
        throw new IllegalArgumentException(
            "LLM API returned HTTP " + response.statusCode() + ": " + response.body());
      }

      return extractContent(response.body());

    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize LLM request", e);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalArgumentException("Failed to reach LLM API: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private String extractContent(String responseBody) {
    try {
      Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
      List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
      if (choices == null || choices.isEmpty()) {
        throw new IllegalArgumentException("LLM API returned no choices");
      }
      // Walk through choices to find the assistant's final text response,
      // skipping tool_call intermediates. Kimi K2.5 may return content in
      // "reasoning" when thinking mode is active, so fall back to that.
      for (Map<String, Object> choice : choices) {
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        if (message == null) continue;
        String role = (String) message.get("role");
        if (!"assistant".equals(role)) continue;

        String content = blankToNull(message.get("content"));
        if (content != null) return content;

        String reasoning = blankToNull(message.get("reasoning"));
        if (reasoning != null) return reasoning;
      }
      log.warn("LLM response body had no usable text: {}", responseBody);
      throw new IllegalArgumentException("LLM API returned no usable assistant message");
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse LLM response", e);
    }
  }

  private List<Map<String, Object>> webSearchTools(String providerUrl) {
    if (providerUrl == null) return List.of();
    String url = providerUrl.toLowerCase(java.util.Locale.ROOT);
    if (url.contains("googleapis.com") || url.contains("generativelanguage")) {
      // Gemini's OpenAI-compat endpoint does not support grounding tools —
      // use the native Gemini SDK if grounding is needed in future.
      return List.of();
    }
    if (url.contains("moonshot.ai") || url.contains("api.nvidia.com")) {
      // Kimi (Moonshot direct or NVIDIA NIM): builtin $web_search
      return List.of(Map.of(
          "type", "builtin_function",
          "function", Map.of("name", "$web_search")
      ));
    }
    if (url.contains("perplexity.ai")) {
      // Perplexity Sonar: web search is always on, no tool needed
      return List.of();
    }
    // Unknown provider — no web search tool added
    return List.of();
  }

  private String stripMarkdownFences(String text) {
    if (text == null) return null;
    return text.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
  }

  private String blankToNull(Object value) {
    if (!(value instanceof String s)) return null;
    return s.isBlank() ? null : s;
  }

  private LlmConfig requireLlmConfig(UUID userId) {
    return userSettingsService.getLlmConfig(userId)
        .orElseThrow(() -> new IllegalArgumentException(
            "No LLM API key configured. Add your API key in Settings."));
  }

  private UserPortfolio verifyOwnership(UUID userId, UUID portfolioEntryId) {
    return userPortfolioRepository
        .findByIdAndUserId(portfolioEntryId, userId)
        .orElseThrow(() -> new IllegalArgumentException("Portfolio entry not found"));
  }

  private IrBriefingResponse toResponse(IrBriefing briefing) {
    return new IrBriefingResponse(
        briefing.getId(),
        briefing.getPortfolioEntryId(),
        briefing.getBriefingJson(),
        briefing.getFetchedAt());
  }

  public record IrBriefingResponse(
      UUID briefingId,
      UUID portfolioEntryId,
      String briefingJson,
      Instant fetchedAt) {}
}
