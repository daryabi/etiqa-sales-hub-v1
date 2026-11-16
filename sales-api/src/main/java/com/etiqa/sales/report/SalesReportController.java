package com.etiqa.sales.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "PDF reporting endpoints powered by JasperReports")
public class SalesReportController {

    private final SalesReportService salesReportService;

    @GetMapping(value = "/sales/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generate a PDF sales report for a date range (YYYY-MM-DD)")
    public PdfDocument downloadSalesPdfReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdfBytes = salesReportService.generateSalesPdf(startDate, endDate);
        return new PdfDocument("sales-report-" + startDate + "-" + endDate + ".pdf", pdfBytes);
    }
}
