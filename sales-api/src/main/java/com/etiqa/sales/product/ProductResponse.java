package com.etiqa.sales.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long bookId,
        String bookTitle,
        BigDecimal bookPrice,
        Integer bookStockQuantity,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getBookId(),
                product.getBookTitle(),
                product.getBookPrice(),
                product.getBookStockQuantity(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
