package com.victorloma.investmentmonitor.user;

import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER', 'BASIC_USER', 'READ_ONLY')")
  @GetMapping("/me")
  public Map<String, Object> me(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of(
        "userId", user.getUserId(),
        "email", user.getEmail(),
        "roles", user.getRoles());
  }
}
