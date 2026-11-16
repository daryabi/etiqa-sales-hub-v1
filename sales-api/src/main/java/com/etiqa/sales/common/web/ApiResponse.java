package com.etiqa.sales.common.web;

import java.time.Instant;

public record ApiResponse<T>(
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", data, Instant.now());
    }

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data, Instant.now());
    }
}
