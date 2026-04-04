package com.victorloma.investmentmonitor.portfolio;

import com.victorloma.investmentmonitor.portfolio.dto.CreatePortfolioEntryRequest;
import com.victorloma.investmentmonitor.portfolio.dto.PortfolioEntryResponse;
import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

  public PortfolioController(PortfolioService portfolioService) {
    this.portfolioService = portfolioService;
  }

  @GetMapping
  public List<PortfolioEntryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
    return portfolioService.listPortfolio(user.getUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioEntryResponse create(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreatePortfolioEntryRequest request) {
    return portfolioService.addPortfolioEntry(user.getUserId(), request);
  }

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
