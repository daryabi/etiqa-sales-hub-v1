package com.etiqa.sales.auth;

public record AuthResponse(
        String token,
        String username,
        String role,
        String timezone
) {
}
