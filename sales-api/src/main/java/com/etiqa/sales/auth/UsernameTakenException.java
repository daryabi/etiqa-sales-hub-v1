package com.etiqa.sales.auth;

import com.etiqa.sales.common.exception.ConflictException;
import com.etiqa.sales.common.exception.ErrorType;

import java.io.Serial;

public class UsernameTakenException extends ConflictException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UsernameTakenException(String message) {
        super(ErrorType.USERNAME_TAKEN, message);
    }
}
