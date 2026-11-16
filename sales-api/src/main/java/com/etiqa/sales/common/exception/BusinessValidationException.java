package com.etiqa.sales.common.exception;

import java.io.Serial;

public class BusinessValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessValidationException(String message) {
        super(message);
    }

    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
