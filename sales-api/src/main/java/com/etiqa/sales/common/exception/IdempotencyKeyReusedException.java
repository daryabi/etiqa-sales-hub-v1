package com.etiqa.sales.common.exception;

import java.io.Serial;

public class IdempotencyKeyReusedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IdempotencyKeyReusedException(String message) {
        super(message);
    }
}
