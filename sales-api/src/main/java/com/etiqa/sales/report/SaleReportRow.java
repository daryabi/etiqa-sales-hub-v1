package com.etiqa.sales.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SaleReportRow {

    private final Long saleId;
    private final String saleDateFormatted;
    private final String bookTitle;
    private final String customerName;
    private final Integer salesQuantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;
}
