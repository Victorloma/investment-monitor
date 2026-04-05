package com.victorloma.investmentmonitor.settings;

import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class UserSettingsController {

  private final UserSettingsService userSettingsService;

  public UserSettingsController(UserSettingsService userSettingsService) {
    this.userSettingsService = userSettingsService;
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @GetMapping
  public UserSettingsResponse getSettings(@AuthenticationPrincipal AuthenticatedUser user) {
    boolean hasKey = userSettingsService.hasLlmApiKey(user.getUserId());
    return userSettingsService.getLlmConfig(user.getUserId())
        .map(c -> new UserSettingsResponse(hasKey, c.providerUrl(), c.modelId()))
        .orElse(new UserSettingsResponse(
            hasKey,
            UserSettingsService.DEFAULT_PROVIDER_URL,
            UserSettingsService.DEFAULT_MODEL_ID));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveSettings(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody SaveSettingsRequest request) {
    userSettingsService.saveSettings(
        user.getUserId(),
        request.apiKey(),
        request.providerUrl(),
        request.modelId());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgument(IllegalArgumentException e) {
    return e.getMessage();
  }

  public record UserSettingsResponse(
      boolean hasApiKey,
      String providerUrl,
      String modelId) {}

  public record SaveSettingsRequest(
      String apiKey,
      String providerUrl,
      String modelId) {}
}
