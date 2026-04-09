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
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IrBriefingService {

  private static final Logger log = LoggerFactory.getLogger(IrBriefingService.class);

  private static final String SYSTEM_PROMPT_TEMPLATE = """
      You are a senior investor relations analyst. Today's date is %s.

      Search for and summarize investor-relevant updates for the given company \
      published in the last 3 months (after %s). Ignore anything older than that.

      Cover:
      - Earnings releases and financial results (revenue, EPS, guidance)
      - SEC filings (8-K, 10-Q, 10-K) with key highlights
      - Material press releases (M&A, leadership changes, product launches)
      - Upcoming earnings calls, investor days, or webcasts

      Return ONLY a valid JSON object in exactly this structure (no markdown, no extra text):
      {
        "asOf": "%s",
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
      Only include items dated after %s. \
      If nothing recent was found, return an empty items array with an honest analystSummary.
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

    String rawContent = isGeminiNative(config.providerUrl())
        ? callGeminiNativeApi(config, userMessage)
        : callOpenAiCompatApi(config, userMessage);

    String briefingJson = stripMarkdownFences(rawContent);

    IrBriefing briefing = new IrBriefing();
    briefing.setPortfolioEntryId(entry.getId());
    briefing.setBriefingJson(briefingJson);
    irBriefingRepository.save(briefing);

    return toResponse(briefing);
  }

  private String buildSystemPrompt() {
    LocalDate today = LocalDate.now();
    LocalDate cutoff = today.minusMonths(3);
    return String.format(SYSTEM_PROMPT_TEMPLATE, today, cutoff, today, cutoff);
  }

  private String buildUserMessage(String companyName, String ticker, String irUrl) {
    StringBuilder sb = new StringBuilder();
    sb.append("Find the latest investor relations updates from the last 3 months for ")
        .append(companyName).append(" (ticker: ").append(ticker).append(").");
    if (irUrl != null && !irUrl.isBlank()) {
      sb.append(" Their IR page is: ").append(irUrl).append(".");
    }
    return sb.toString();
  }

  // ── Gemini native path ────────────────────────────────────────────────────

  private boolean isGeminiNative(String providerUrl) {
    if (providerUrl == null) return false;
    String url = providerUrl.toLowerCase(Locale.ROOT);
    return url.contains("googleapis.com") || url.contains("generativelanguage");
  }

  private String callGeminiNativeApi(LlmConfig config, String userMessage) {
    try {
      String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
          + config.modelId() + ":generateContent?key=" + config.apiKey();

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("systemInstruction", Map.of(
          "parts", List.of(Map.of("text", buildSystemPrompt()))
      ));
      body.put("contents", List.of(Map.of(
          "role", "user",
          "parts", List.of(Map.of("text", userMessage))
      )));
      body.put("tools", List.of(Map.of("googleSearch", Map.of())));

      String requestBody = objectMapper.writeValueAsString(body);
      log.info("Calling Gemini native API: POST {} model={}", apiUrl.split("\\?")[0], config.modelId());

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .timeout(Duration.ofSeconds(60))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("Gemini native API response: HTTP {}", response.statusCode());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Gemini native API error body: {}", response.body());
        throw new IllegalArgumentException(
            "LLM API returned HTTP " + response.statusCode() + ": " + response.body());
      }

      return extractGeminiContent(response.body());

    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize Gemini request", e);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new IllegalArgumentException("Failed to reach Gemini API: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private String extractGeminiContent(String responseBody) {
    try {
      Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
      List<Map<String, Object>> candidates = (List<Map<String, Object>>) parsed.get("candidates");
      if (candidates == null || candidates.isEmpty()) {
        log.warn("Gemini response had no candidates: {}", responseBody);
        throw new IllegalArgumentException("Gemini API returned no candidates");
      }
      Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
      if (content == null) throw new IllegalArgumentException("Gemini candidate had no content");
      List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
      if (parts == null || parts.isEmpty()) throw new IllegalArgumentException("Gemini content had no parts");

      // Concatenate all text parts (grounding may split the response)
      StringBuilder text = new StringBuilder();
      for (Map<String, Object> part : parts) {
        Object t = part.get("text");
        if (t instanceof String s && !s.isBlank()) text.append(s);
      }
      String result = text.toString().trim();
      if (result.isBlank()) {
        log.warn("Gemini response parts had no text: {}", responseBody);
        throw new IllegalArgumentException("Gemini API returned no text content");
      }
      return result;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse Gemini response", e);
    }
  }

  // ── OpenAI-compatible path ────────────────────────────────────────────────

  private String callOpenAiCompatApi(LlmConfig config, String userMessage) {
    try {
      List<Map<String, Object>> tools = openAiWebSearchTools(config.providerUrl());

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", config.modelId());
      body.put("messages", List.of(
          Map.of("role", "system", "content", buildSystemPrompt()),
          Map.of("role", "user", "content", userMessage)
      ));
      if (!tools.isEmpty()) body.put("tools", tools);
      body.put("max_tokens", 1024);
      body.put("temperature", 1.0);
      body.put("top_p", 1.0);
      body.put("stream", false);

      String requestBody = objectMapper.writeValueAsString(Collections.unmodifiableMap(body));
      String base = config.providerUrl().strip().replaceAll("/+$", "");
      String apiUrl = base.endsWith("/chat/completions") ? base : base + "/chat/completions";

      log.info("Calling OpenAI-compat API: POST {} model={}", apiUrl, config.modelId());

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiUrl))
          .timeout(Duration.ofSeconds(300))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("Authorization", "Bearer " + config.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("OpenAI-compat API response: HTTP {}", response.statusCode());

      if (response.statusCode() == 401) {
        log.warn("OpenAI-compat API 401 body: {}", response.body());
        throw new IllegalArgumentException("LLM API key is invalid or expired.");
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("OpenAI-compat API error body: {}", response.body());
        throw new IllegalArgumentException(
            "LLM API returned HTTP " + response.statusCode() + ": " + response.body());
      }

      return extractOpenAiContent(response.body());

    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize LLM request", e);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new IllegalArgumentException("Failed to reach LLM API: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private String extractOpenAiContent(String responseBody) {
    try {
      Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
      List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
      if (choices == null || choices.isEmpty()) {
        throw new IllegalArgumentException("LLM API returned no choices");
      }
      for (Map<String, Object> choice : choices) {
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        if (message == null) continue;
        if (!"assistant".equals(message.get("role"))) continue;
        String content = blankToNull(message.get("content"));
        if (content != null) return content;
        String reasoning = blankToNull(message.get("reasoning"));
        if (reasoning != null) return reasoning;
      }
      log.warn("OpenAI-compat response had no usable text: {}", responseBody);
      throw new IllegalArgumentException("LLM API returned no usable assistant message");
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse LLM response", e);
    }
  }

  private List<Map<String, Object>> openAiWebSearchTools(String providerUrl) {
    if (providerUrl == null) return List.of();
    String url = providerUrl.toLowerCase(Locale.ROOT);
    if (url.contains("moonshot.ai") || url.contains("api.nvidia.com")) {
      return List.of(Map.of(
          "type", "builtin_function",
          "function", Map.of("name", "$web_search")
      ));
    }
    // Perplexity: web search always on, no tool needed
    // Groq / others: no web search
    return List.of();
  }

  // ── Shared helpers ────────────────────────────────────────────────────────

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
