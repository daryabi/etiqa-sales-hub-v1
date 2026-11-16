package com.etiqa.sales.product;

public record StockLevelChanged(
        Long bookId,
        String bookTitle,
        Integer previousStock,
        Integer remainingStock
) {
}
