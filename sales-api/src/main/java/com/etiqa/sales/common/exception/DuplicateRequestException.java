package com.etiqa.sales.common.exception;

import java.io.Serial;

public class DuplicateRequestException extends ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateRequestException(String message) {
        super(ErrorType.DUPLICATE_REQUEST, message);
    }
}
