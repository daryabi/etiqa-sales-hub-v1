package com.etiqa.sales.report;

import com.etiqa.sales.support.TestAppProperties;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.sale.SaleResponse;
import com.etiqa.sales.sale.SaleService;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

    private static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    private static final LocalDate NOV_16 = LocalDate.of(2026, 11, 16);

    @Mock
    private SaleService saleService;

    private SalesReportService reportService;

    @BeforeAll
    static void runJasperHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() {
        reportService = new SalesReportService(
                saleService, TestAppProperties.withDefaults(), TestMessages.create());
        reportService.compileReportTemplate();
    }

    @Test
    void reportDay_runsFromMalaysianMidnightToMidnight() {
        Instant startOfNov16InMalaysia = Instant.parse("2026-11-15T16:00:00Z");
        Instant startOfNov17InMalaysia = Instant.parse("2026-11-16T16:00:00Z");
        SaleResponse earlyMorningSale = new SaleResponse(1L, Instant.parse("2026-11-15T16:30:00Z"), 10L, "Effective Java",
                1L, "Alice Tan", 2, new BigDecimal("45.50"), new BigDecimal("91.00"));
        when(saleService.findReportLines(startOfNov16InMalaysia, startOfNov17InMalaysia)).thenReturn(List.of(earlyMorningSale));

        byte[] pdf = reportService.generateSalesPdf(NOV_16, NOV_16);

        verify(saleService).findReportLines(startOfNov16InMalaysia, startOfNov17InMalaysia);
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void reportHeader_namesTheTimezone() {
        assertThat(SalesReportService.zoneLabel(MALAYSIA)).isEqualTo("MYT / Asia/Kuala_Lumpur");
    }

    @Test
    void rejectsStartDateAfterEndDate() {
        assertThatThrownBy(() -> reportService.generateSalesPdf(NOV_16, NOV_16.minusDays(1)))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(saleService);
    }

    @Test
    void allowsAFullYearButNotOneDayMore() {
        when(saleService.findReportLines(any(), any())).thenReturn(List.of());

        assertThat(reportService.generateSalesPdf(NOV_16, NOV_16.plusDays(365))).isNotEmpty();
        assertThatThrownBy(() -> reportService.generateSalesPdf(NOV_16, NOV_16.plusDays(366)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("A report can cover at most 366 days.");
    }
}
