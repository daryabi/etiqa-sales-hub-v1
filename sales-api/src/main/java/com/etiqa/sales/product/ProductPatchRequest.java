package com.etiqa.sales.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductPatchRequest(
        @Size(max = Product.TITLE_MAX_LENGTH) String bookTitle,
        @DecimalMin(Product.MIN_PRICE) BigDecimal bookPrice,
        @Min(Product.MIN_STOCK) Integer bookStockQuantity,
        Boolean isActive
) {
}
