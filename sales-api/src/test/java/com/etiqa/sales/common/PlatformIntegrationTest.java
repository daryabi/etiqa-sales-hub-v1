package com.etiqa.sales.common;

import com.etiqa.sales.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformIntegrationTest extends ApiIntegrationTest {

    @Test
    void frameworkErrors_mapToClientStatuses() {
        Consumer<HttpHeaders> admin = bearer(login("admin", "admin123"));

        assertThat(call(HttpMethod.PUT, "/api/v1/audit-logs", admin, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(call(HttpMethod.GET, "/api/v1/does-not-exist", admin, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/v1/customers/abc", admin, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "/api/v1/customers?sort=unknownField,asc", admin, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "/api/v1/audit-logs?entityName=BOGUS", admin, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Reply<Object> unsupported = call(HttpMethod.POST, "/api/v1/customers",
                admin.andThen(headers -> headers.setContentType(MediaType.TEXT_PLAIN)), "not json", ANY_BODY);
        assertThat(unsupported.status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void prometheus_needsScrapeAccount() {
        assertThat(call(HttpMethod.GET, "/actuator/prometheus", noAuth(), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "/actuator/prometheus",
                headers -> headers.setBasicAuth("prometheus", "wrong-password"), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        Reply<byte[]> scrape = call(HttpMethod.GET, "/actuator/prometheus",
                headers -> headers.setBasicAuth(securityProperties.metrics().username(), securityProperties.metrics().password()),
                null, BYTES);
        assertThat(scrape.status()).isEqualTo(HttpStatus.OK);
        assertThat(new String(scrape.body(), StandardCharsets.UTF_8)).contains("jvm_memory_used_bytes");
    }

    @Test
    void apiDocs_useBearerAuthAndDocumentProblems() {
        Reply<byte[]> docs = call(HttpMethod.GET, "/v3/api-docs", noAuth(), null, BYTES);
        String spec = new String(docs.body(), StandardCharsets.UTF_8);

        assertThat(docs.status()).isEqualTo(HttpStatus.OK);
        assertThat(spec).doesNotContain("\"name\":\"token\"");
        assertThat(spec).contains("\"422\"", "application/problem+json", "#/components/schemas/ProblemDetail");
    }
}
