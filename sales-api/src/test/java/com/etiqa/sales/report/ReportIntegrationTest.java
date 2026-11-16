package com.etiqa.sales.report;

import com.etiqa.sales.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ReportIntegrationTest extends ApiIntegrationTest {

    @Test
    void periodLongerThanAYear_isRejected() {
        Reply<Object> tooLong = call(HttpMethod.GET, "/api/v1/reports/sales/pdf?startDate=2025-01-01&endDate=2026-01-02",
                bearer(login("staff", "staff123")), null, ANY_BODY);

        assertThat(tooLong.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooLong.problem().getDetail()).isEqualTo("A report can cover at most 366 days.");
    }
}
