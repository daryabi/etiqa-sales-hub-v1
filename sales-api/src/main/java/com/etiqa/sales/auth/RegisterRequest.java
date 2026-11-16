package com.etiqa.sales.auth;

import com.etiqa.sales.common.i18n.MessageKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = MessageKey.VALIDATION_USERNAME_REQUIRED)
        @Size(min = UserAccount.USERNAME_MIN_LENGTH, max = UserAccount.USERNAME_MAX_LENGTH,
                message = MessageKey.VALIDATION_USERNAME_LENGTH)
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = MessageKey.VALIDATION_USERNAME_CHARACTERS)
        String username,

        @NotBlank(message = MessageKey.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = UserAccount.PASSWORD_MIN_LENGTH, max = UserAccount.PASSWORD_MAX_LENGTH,
                message = MessageKey.VALIDATION_PASSWORD_LENGTH)
        String password
) {
}
