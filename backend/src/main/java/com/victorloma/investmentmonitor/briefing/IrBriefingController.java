package com.victorloma.investmentmonitor.briefing;

import com.victorloma.investmentmonitor.briefing.IrBriefingService.IrBriefingResponse;
import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class IrBriefingController {

  private final IrBriefingService irBriefingService;

  public IrBriefingController(IrBriefingService irBriefingService) {
    this.irBriefingService = irBriefingService;
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @GetMapping("/{portfolioEntryId}/briefing")
  public IrBriefingResponse getLatestBriefing(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID portfolioEntryId) {
    return irBriefingService.getLatestBriefing(user.getUserId(), portfolioEntryId)
        .orElse(null);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @PostMapping("/{portfolioEntryId}/briefing")
  public IrBriefingResponse fetchBriefing(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID portfolioEntryId) {
    return irBriefingService.fetchAndSaveBriefing(user.getUserId(), portfolioEntryId);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @PostMapping("/briefing/refresh-all")
  public List<IrBriefingResponse> refreshAll(@AuthenticationPrincipal AuthenticatedUser user) {
    return irBriefingService.fetchAndSaveAllBriefings(user.getUserId());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgument(IllegalArgumentException e) {
    return e.getMessage();
  }
}
