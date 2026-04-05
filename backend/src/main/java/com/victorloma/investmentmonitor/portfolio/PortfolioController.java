package com.victorloma.investmentmonitor.portfolio;

import com.victorloma.investmentmonitor.portfolio.dto.CreatePortfolioEntryRequest;
import com.victorloma.investmentmonitor.portfolio.dto.PortfolioCrawlPreviewResponse;
import com.victorloma.investmentmonitor.portfolio.dto.PortfolioEntryResponse;
import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final IrCrawlService irCrawlService;

  public PortfolioController(PortfolioService portfolioService, IrCrawlService irCrawlService) {
    this.portfolioService = portfolioService;
    this.irCrawlService = irCrawlService;
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER', 'READ_ONLY')")
  @GetMapping
  public List<PortfolioEntryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
    return portfolioService.listPortfolio(user.getUserId());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioEntryResponse create(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreatePortfolioEntryRequest request) {
    return portfolioService.addPortfolioEntry(user.getUserId(), request);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @GetMapping("/{portfolioEntryId}/crawl-preview")
  public PortfolioCrawlPreviewResponse crawlPreview(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID portfolioEntryId) {
    return irCrawlService.previewPortfolioEntryIrLinks(user.getUserId(), portfolioEntryId);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER')")
  @DeleteMapping("/{portfolioEntryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID portfolioEntryId) {
    portfolioService.removePortfolioEntry(user.getUserId(), portfolioEntryId);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgument(IllegalArgumentException exception) {
    return exception.getMessage();
  }
}
