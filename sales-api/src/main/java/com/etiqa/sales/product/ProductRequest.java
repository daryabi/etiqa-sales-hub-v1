package com.etiqa.sales.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = Product.TITLE_MAX_LENGTH) String bookTitle,
        @NotNull @DecimalMin(Product.MIN_PRICE) BigDecimal bookPrice,
        @NotNull @Min(Product.MIN_STOCK) Integer bookStockQuantity
) {
}
