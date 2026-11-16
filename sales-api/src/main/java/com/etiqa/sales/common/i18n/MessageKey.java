package com.etiqa.sales.common.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageKey {

    CUSTOMER_OFFICE_EMAIL_DUPLICATE("customer.email.office.duplicate"),
    CUSTOMER_PERSONAL_EMAIL_DUPLICATE("customer.email.personal.duplicate"),
    CUSTOMER_EMAILS_IDENTICAL("customer.email.identical"),
    CUSTOMER_NOT_FOUND("customer.not.found"),

    PRODUCT_NOT_FOUND("product.not.found"),
    PRODUCT_IMPORT_EMPTY("product.import.empty"),
    PRODUCT_IMPORT_READ_ERROR("product.import.read.error"),
    PRODUCT_IMPORT_LINE_COLUMNS("product.import.line.columns"),
    PRODUCT_IMPORT_LINE_TITLE("product.import.line.title"),
    PRODUCT_IMPORT_LINE_RANGE("product.import.line.range"),
    PRODUCT_IMPORT_LINE_FORMAT("product.import.line.format"),
    PRODUCT_IMPORT_LINE_DUPLICATE("product.import.line.duplicate"),

    SALE_NOT_FOUND("sale.not.found"),
    SALE_PRODUCT_INACTIVE("sale.product.inactive"),
    SALE_STOCK_INSUFFICIENT("sale.stock.insufficient"),
    SALE_IDEMPOTENCY_KEY_TOO_LONG("sale.idempotency.key.too.long"),
    SALE_REQUEST_IN_PROGRESS("sale.request.in.progress"),
    SALE_IDEMPOTENCY_KEY_REUSED("sale.idempotency.key.reused"),

    REPORT_DATE_INVALID("report.date.invalid"),
    REPORT_RANGE_TOO_LONG("report.range.too.long"),
    REPORT_TITLE("report.title"),
    REPORT_PERIOD("report.period"),

    USER_NOT_FOUND("user.not.found"),
    USER_USERNAME_TAKEN("user.username.taken"),

    BOOK_LOOKUP_ISBN_INVALID("book.lookup.isbn.invalid"),
    BOOK_LOOKUP_NOT_FOUND("book.lookup.not.found"),
    BOOK_LOOKUP_UNAVAILABLE("book.lookup.unavailable"),

    ERROR_UNAUTHORIZED_TITLE("error.unauthorized.title"),
    ERROR_UNAUTHORIZED_DETAIL("error.unauthorized.detail"),
    ERROR_UNAUTHORIZED_JWT_DETAIL("error.unauthorized.jwt.detail"),
    ERROR_FORBIDDEN_TITLE("error.forbidden.title"),
    ERROR_FORBIDDEN_DETAIL("error.forbidden.detail"),
    ERROR_NOT_FOUND_TITLE("error.not-found.title"),
    ERROR_ENDPOINT_NOT_FOUND_TITLE("error.endpoint-not-found.title"),
    ERROR_BUSINESS_RULE_TITLE("error.business-rule.title"),
    ERROR_INSUFFICIENT_STOCK_TITLE("error.insufficient-stock.title"),
    ERROR_USERNAME_TAKEN_TITLE("error.username-taken.title"),
    ERROR_NOT_ACCEPTABLE_TITLE("error.not-acceptable.title"),
    ERROR_PAYLOAD_TOO_LARGE_TITLE("error.payload-too-large.title"),
    ERROR_PAYLOAD_TOO_LARGE_DETAIL("error.payload-too-large.detail"),
    ERROR_VALIDATION_TITLE("error.validation.title"),
    ERROR_VALIDATION_DETAIL("error.validation.detail"),
    ERROR_METHOD_NOT_ALLOWED_TITLE("error.method-not-allowed.title"),
    ERROR_BAD_REQUEST_TITLE("error.bad-request.title"),
    ERROR_UNSUPPORTED_MEDIA_TYPE_TITLE("error.unsupported-media-type.title"),
    ERROR_CONFLICT_TITLE("error.conflict.title"),
    ERROR_CONFLICT_DETAIL("error.conflict.detail"),
    ERROR_DATA_INTEGRITY_TITLE("error.data-integrity.title"),
    ERROR_DATA_INTEGRITY_DETAIL("error.data-integrity.detail"),
    ERROR_DUPLICATE_REQUEST_TITLE("error.duplicate-request.title"),
    ERROR_IDEMPOTENCY_KEY_REUSED_TITLE("error.idempotency-key-reused.title"),
    ERROR_UPSTREAM_UNAVAILABLE_TITLE("error.upstream-unavailable.title"),
    ERROR_INTERNAL_TITLE("error.internal.title"),
    ERROR_INTERNAL_DETAIL("error.internal.detail");

    public static final String VALIDATION_USERNAME_REQUIRED = "{auth.username.required}";
    public static final String VALIDATION_USERNAME_LENGTH = "{auth.username.length}";
    public static final String VALIDATION_USERNAME_CHARACTERS = "{auth.username.characters}";
    public static final String VALIDATION_PASSWORD_REQUIRED = "{auth.password.required}";
    public static final String VALIDATION_PASSWORD_LENGTH = "{auth.password.length}";
    public static final String VALIDATION_SALE_QUANTITY_MIN = "{sale.quantity.min}";

    private final String key;
}
