package com.etiqa.sales.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, @DefaultValue Metrics metrics) {

    public record Jwt(String secret, long expirationMs) {
    }

    public record Metrics(@DefaultValue("prometheus") String username, @DefaultValue("") String password) {

        public boolean scrapeEnabled() {
            return !username.isBlank() && !password.isBlank();
        }
    }
}
