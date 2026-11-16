package com.etiqa.sales.sale;

import com.etiqa.sales.common.i18n.MessageKey;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleRequest(
        @NotNull Long customerId,
        @NotNull Long bookId,
        @NotNull @Min(value = Sale.MIN_QUANTITY, message = MessageKey.VALIDATION_SALE_QUANTITY_MIN) Integer salesQuantity
) {
}
