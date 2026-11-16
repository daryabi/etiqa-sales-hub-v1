package com.etiqa.sales.sale;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleResponse(
        Long saleId,
        Instant saleDate,
        Long bookId,
        String bookTitle,
        Long customerId,
        String customerName,
        Integer salesQuantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {
}
