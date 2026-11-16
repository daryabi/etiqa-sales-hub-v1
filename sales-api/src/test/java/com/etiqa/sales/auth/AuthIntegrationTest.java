package com.etiqa.sales.auth;

import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends ApiIntegrationTest {

    @Test
    void authAndAdminBoundaries() {
        Reply<Object> unauthenticated = call(HttpMethod.GET, "/api/v1/sales", noAuth(), null, ANY_BODY);
        assertThat(unauthenticated.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.headers().getFirst("X-Correlation-ID")).isNotBlank();
        assertThat(unauthenticated.problem().getTitle()).isEqualTo("Authentication Failed");
        assertThat(unauthenticated.problem().getType().toString()).endsWith("/errors/unauthorized");
        assertThat(unauthenticated.problem().getProperties()).containsKey("correlationId");

        assertThat(call(HttpMethod.GET, "/api/v1/products", noAuth(), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/actuator/health", noAuth(), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/v3/api-docs", noAuth(), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.OK);

        Reply<Object> forgedToken = call(HttpMethod.GET, "/api/v1/sales", bearer("not.a.jwt"), null, ANY_BODY);
        assertThat(forgedToken.status()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Reply<Object> badLogin = call(HttpMethod.POST, "/api/v1/auth/login", noAuth(),
                new LoginRequest("staff", "wrong-password"), ANY_BODY);
        assertThat(badLogin.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(badLogin.problem().getDetail()).isEqualTo("Invalid username or password.");

        Reply<Object> staffAudit = call(HttpMethod.GET, "/api/v1/audit-logs", bearer(login("staff", "staff123")), null, ANY_BODY);
        assertThat(staffAudit.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(staffAudit.problem().getStatus()).isEqualTo(403);
    }

    @Test
    void signUp_alwaysCreatesStaff() {
        Reply<ApiResponse<AuthResponse>> signUp = call(HttpMethod.POST, "/api/v1/auth/register", noAuth(),
                Map.of("username", "new.cashier", "password", "secret12", "role", "ADMIN"),
                envelope(AuthResponse.class));
        assertThat(signUp.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(signUp.body().message()).isEqualTo("Account created");
        assertThat(signUp.body().data().username()).isEqualTo("new.cashier");
        assertThat(signUp.body().data().role()).isEqualTo("ROLE_STAFF");
        assertThat(signUp.body().data().token()).isNotBlank();

        Reply<Object> duplicate = call(HttpMethod.POST, "/api/v1/auth/register", noAuth(),
                new RegisterRequest("new.cashier", "another1"), ANY_BODY);
        assertThat(duplicate.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.problem().getTitle()).isEqualTo("Username Taken");
        assertThat(duplicate.problem().getDetail()).contains("new.cashier");

        Reply<Object> invalid = call(HttpMethod.POST, "/api/v1/auth/register", noAuth(),
                new RegisterRequest("ab", "123"), ANY_BODY);
        assertThat(invalid.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.problem().getProperties()).containsKey("errors");

        String newUserToken = login("new.cashier", "secret12");
        assertThat(call(HttpMethod.GET, "/api/v1/sales", bearer(newUserToken), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/api/v1/audit-logs", bearer(newUserToken), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deletedUsersToken_getsCleanUnauthorized() {
        data(call(HttpMethod.POST, "/api/v1/auth/register", noAuth(),
                new RegisterRequest("leaving.cashier", "secret12"), envelope(AuthResponse.class)));
        String token = login("leaving.cashier", "secret12");
        userAccountRepository.delete(userAccountRepository.findByUsername("leaving.cashier").orElseThrow());

        Reply<Object> afterDeletion = call(HttpMethod.GET, "/api/v1/sales", bearer(token), null, ANY_BODY);

        assertThat(afterDeletion.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(afterDeletion.problem().getTitle()).isEqualTo("Authentication Failed");
    }

    @Test
    void login_returnsTheBusinessTimezone() {
        AuthResponse session = data(call(HttpMethod.POST, "/api/v1/auth/login", noAuth(),
                new LoginRequest("staff", "staff123"), envelope(AuthResponse.class)));

        assertThat(session.timezone()).isEqualTo("Asia/Kuala_Lumpur");
    }
}
