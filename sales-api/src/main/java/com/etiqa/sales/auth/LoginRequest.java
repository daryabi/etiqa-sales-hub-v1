package com.etiqa.sales.auth;

import com.etiqa.sales.common.i18n.MessageKey;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = MessageKey.VALIDATION_USERNAME_REQUIRED) String username,
        @NotBlank(message = MessageKey.VALIDATION_PASSWORD_REQUIRED) String password
) {
}
