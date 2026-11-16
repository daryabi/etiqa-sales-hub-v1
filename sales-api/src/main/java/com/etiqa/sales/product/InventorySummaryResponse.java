package com.etiqa.sales.product;

import java.util.Objects;

public record InventorySummaryResponse(
        Long activeTitles,
        Long copiesInStock
) {
    public InventorySummaryResponse {
        activeTitles = Objects.requireNonNullElse(activeTitles, 0L);
        copiesInStock = Objects.requireNonNullElse(copiesInStock, 0L);
    }
}
