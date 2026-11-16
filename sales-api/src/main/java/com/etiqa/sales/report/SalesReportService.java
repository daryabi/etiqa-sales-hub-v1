package com.etiqa.sales.report;

import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import com.etiqa.sales.sale.SaleResponse;
import com.etiqa.sales.sale.SaleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesReportService {

    private static final String TEMPLATE_PATH = "reports/sales_report.jrxml";
    private static final DateTimeFormatter SALE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SaleService saleService;
    private final AppProperties appProperties;
    private final Messages messages;

    private JasperReport salesReportTemplate;

    @PostConstruct
    public void compileReportTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream jrxmlStream = resource.getInputStream()) {
            this.salesReportTemplate = JasperCompileManager.compileReport(jrxmlStream);
            log.info("Compiled JasperReports template: {}", TEMPLATE_PATH);
        } catch (IOException | JRException exception) {
            throw new IllegalStateException("Failed to compile JasperReports sales template", exception);
        }
    }

    public byte[] generateSalesPdf(LocalDate startDate, LocalDate endDate) {
        checkPeriod(startDate, endDate);

        // Days are cut at Malaysian midnight. With UTC days, a 12:30am sale would show up in yesterday's report.
        ZoneId businessZone = appProperties.timezone();
        Instant periodStart = startDate.atStartOfDay(businessZone).toInstant();
        Instant periodEndExclusive = endDate.plusDays(1).atStartOfDay(businessZone).toInstant();

        List<SaleResponse> sales = saleService.findReportLines(periodStart, periodEndExclusive);
        DateTimeFormatter saleDateFormat = SALE_DATE_FORMAT.withZone(businessZone);
        List<SaleReportRow> reportLines = sales.stream()
                .map(sale -> new SaleReportRow(
                        sale.saleId(),
                        saleDateFormat.format(sale.saleDate()),
                        sale.bookTitle(),
                        sale.customerName(),
                        sale.salesQuantity(),
                        sale.unitPrice(),
                        sale.totalAmount()))
                .toList();

        Map<String, Object> reportParameters = new HashMap<>();
        reportParameters.put("REPORT_TITLE", messages.get(MessageKey.REPORT_TITLE));
        reportParameters.put("DATE_RANGE", messages.get(MessageKey.REPORT_PERIOD, zoneLabel(businessZone), startDate, endDate));
        reportParameters.put("TOTAL_ITEMS", sales.stream().mapToInt(SaleResponse::salesQuantity).sum());
        reportParameters.put("GRAND_TOTAL", sales.stream().map(SaleResponse::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        try {
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportLines);
            JasperPrint jasperPrint = JasperFillManager.fillReport(this.salesReportTemplate, reportParameters, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException exception) {
            throw new IllegalStateException("Failed to render PDF sales report", exception);
        }
    }

    private void checkPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessValidationException(messages.get(MessageKey.REPORT_DATE_INVALID));
        }
        long daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInPeriod > appProperties.maxReportDays()) {
            throw new BusinessValidationException(messages.get(MessageKey.REPORT_RANGE_TOO_LONG, appProperties.maxReportDays()));
        }
    }

    static String zoneLabel(ZoneId zone) {
        return zone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " / " + zone.getId();
    }
}
