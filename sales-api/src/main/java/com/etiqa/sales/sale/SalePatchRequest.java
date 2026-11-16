package com.etiqa.sales.sale;

import com.etiqa.sales.common.i18n.MessageKey;
import jakarta.validation.constraints.Min;

public record SalePatchRequest(
        Long customerId,
        Long bookId,
        @Min(value = Sale.MIN_QUANTITY, message = MessageKey.VALIDATION_SALE_QUANTITY_MIN) Integer salesQuantity
) {
}
