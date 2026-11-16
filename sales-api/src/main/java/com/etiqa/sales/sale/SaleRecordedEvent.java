package com.etiqa.sales.sale;

import java.math.BigDecimal;

public record SaleRecordedEvent(
        Long saleId,
        Long bookId,
        Long customerId,
        Integer salesQuantity,
        BigDecimal totalAmount
) {
}
