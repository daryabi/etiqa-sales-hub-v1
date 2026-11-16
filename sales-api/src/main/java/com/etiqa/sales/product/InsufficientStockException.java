package com.etiqa.sales.product;

import com.etiqa.sales.common.exception.ConflictException;
import com.etiqa.sales.common.exception.ErrorType;

import java.io.Serial;

public class InsufficientStockException extends ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super(ErrorType.INSUFFICIENT_STOCK, message);
    }
}
