package com.victorloma.investmentmonitor.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        List<String> roles,
        String accessToken
) {
}
