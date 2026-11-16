package com.etiqa.sales.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SIGNING_KEY = base64("an-hmac-key-that-is-definitely-longer-than-32-bytes");
    private static final String OTHER_SIGNING_KEY = base64("a-different-hmac-key-also-longer-than-32-bytes!!");
    private static final long ONE_HOUR_MS = 3_600_000L;

    private final UserDetails staff = User.withUsername("staff").password("unused").roles("STAFF").build();

    @Test
    void roundTrip_returnsUsername() {
        JwtService jwtService = jwtService(SIGNING_KEY, ONE_HOUR_MS);

        assertThat(jwtService.readUsernameIfValid(jwtService.generateToken(staff))).isEqualTo("staff");
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwtService = jwtService(SIGNING_KEY, -ONE_HOUR_MS);
        String expired = jwtService.generateToken(staff);

        assertThatThrownBy(() -> jwtService.readUsernameIfValid(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void foreignSignature_isRejected() {
        String foreignToken = jwtService(OTHER_SIGNING_KEY, ONE_HOUR_MS).generateToken(staff);

        assertThatThrownBy(() -> jwtService(SIGNING_KEY, ONE_HOUR_MS).readUsernameIfValid(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void missingSecret_failsFast() {
        assertThatThrownBy(() -> jwtService(null, ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be set");
        assertThatThrownBy(() -> jwtService("  ", ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> jwtService(base64("too-short"), ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must decode to at least 32 bytes");
    }

    private static JwtService jwtService(String secret, long expirationMs) {
        return new JwtService(new SecurityProperties(
                new SecurityProperties.Jwt(secret, expirationMs),
                new SecurityProperties.Metrics("prometheus", "")
        ));
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
