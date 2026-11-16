package com.etiqa.sales.sale;

import java.math.BigDecimal;
import java.util.Objects;

public record SalesSummaryResponse(
        Long orderCount,
        Long unitsSold,
        BigDecimal revenue
) {
    public SalesSummaryResponse {
        orderCount = Objects.requireNonNullElse(orderCount, 0L);
        unitsSold = Objects.requireNonNullElse(unitsSold, 0L);
        revenue = Objects.requireNonNullElse(revenue, BigDecimal.ZERO);
    }
}
