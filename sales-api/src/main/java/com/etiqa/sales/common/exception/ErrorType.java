package com.etiqa.sales.common.exception;

import com.etiqa.sales.common.i18n.MessageKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "unauthorized", MessageKey.ERROR_UNAUTHORIZED_TITLE, MessageKey.ERROR_UNAUTHORIZED_DETAIL),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "unauthorized", MessageKey.ERROR_UNAUTHORIZED_TITLE, MessageKey.ERROR_UNAUTHORIZED_JWT_DETAIL),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", MessageKey.ERROR_FORBIDDEN_TITLE, MessageKey.ERROR_FORBIDDEN_DETAIL),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "not-found", MessageKey.ERROR_NOT_FOUND_TITLE, null),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "not-found", MessageKey.ERROR_ENDPOINT_NOT_FOUND_TITLE, null),
    BUSINESS_RULE(HttpStatus.BAD_REQUEST, "business-rule", MessageKey.ERROR_BUSINESS_RULE_TITLE, null),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "insufficient-stock", MessageKey.ERROR_INSUFFICIENT_STOCK_TITLE, null),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "username-taken", MessageKey.ERROR_USERNAME_TAKEN_TITLE, null),
    VALIDATION(HttpStatus.BAD_REQUEST, "validation", MessageKey.ERROR_VALIDATION_TITLE, MessageKey.ERROR_VALIDATION_DETAIL),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "bad-request", MessageKey.ERROR_BAD_REQUEST_TITLE, null),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", MessageKey.ERROR_METHOD_NOT_ALLOWED_TITLE, null),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type", MessageKey.ERROR_UNSUPPORTED_MEDIA_TYPE_TITLE, null),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "not-acceptable", MessageKey.ERROR_NOT_ACCEPTABLE_TITLE, null),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "payload-too-large", MessageKey.ERROR_PAYLOAD_TOO_LARGE_TITLE, MessageKey.ERROR_PAYLOAD_TOO_LARGE_DETAIL),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency-key-reused", MessageKey.ERROR_IDEMPOTENCY_KEY_REUSED_TITLE, null),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "duplicate-request", MessageKey.ERROR_DUPLICATE_REQUEST_TITLE, null),
    UPSTREAM_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "upstream-unavailable", MessageKey.ERROR_UPSTREAM_UNAVAILABLE_TITLE, null),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "conflict", MessageKey.ERROR_CONFLICT_TITLE, MessageKey.ERROR_CONFLICT_DETAIL),
    DATA_INTEGRITY(HttpStatus.CONFLICT, "data-integrity", MessageKey.ERROR_DATA_INTEGRITY_TITLE, MessageKey.ERROR_DATA_INTEGRITY_DETAIL),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "internal", MessageKey.ERROR_INTERNAL_TITLE, MessageKey.ERROR_INTERNAL_DETAIL);

    private final HttpStatus status;
    private final String typeName;
    private final MessageKey titleKey;
    private final MessageKey detailKey;
}
