package com.etiqa.sales.sale;

import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.DuplicateRequestException;
import com.etiqa.sales.common.exception.IdempotencyKeyReusedException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaleIdempotencyGuard {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final String IN_PROGRESS = "in-progress";
    private static final String SEPARATOR = "|";
    static final Duration IN_PROGRESS_LIFETIME = Duration.ofMinutes(2);
    static final Duration RESULT_LIFETIME = Duration.ofHours(24);
    private static final int MAX_KEY_LENGTH = 100;

    private final StringRedisTemplate redisTemplate;
    private final SaleService saleService;
    private final Messages messages;

    public SaleResponse recordOnce(String username, String idempotencyKey, SaleRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return saleService.recordSale(request);
        }
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new BusinessValidationException(messages.get(MessageKey.SALE_IDEMPOTENCY_KEY_TOO_LONG, MAX_KEY_LENGTH));
        }

        String redisKey = "idemp:" + username + ":" + idempotencyKey;
        String requestHash = fingerprint(request);
        Boolean reserved;
        try {
            reserved = redisTemplate.opsForValue().setIfAbsent(redisKey, IN_PROGRESS + SEPARATOR + requestHash, IN_PROGRESS_LIFETIME);
        } catch (DataAccessException exception) {
            log.warn("Redis is unavailable, recording sale without an idempotency check: {}", exception.getMessage());
            return saleService.recordSale(request);
        }
        if (!Boolean.TRUE.equals(reserved)) {
            return saleAlreadyRecordedFor(redisKey, requestHash);
        }

        SaleResponse sale;
        try {
            sale = saleService.recordSale(request);
        } catch (RuntimeException exception) {
            forget(redisKey);
            throw exception;
        }
        remember(redisKey, sale.saleId() + SEPARATOR + requestHash);
        return sale;
    }

    private SaleResponse saleAlreadyRecordedFor(String redisKey, String requestHash) {
        String stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            throw new DuplicateRequestException(messages.get(MessageKey.SALE_REQUEST_IN_PROGRESS));
        }
        int separatorAt = stored.indexOf(SEPARATOR);
        if (separatorAt < 0) {
            throw new DuplicateRequestException(messages.get(MessageKey.SALE_REQUEST_IN_PROGRESS));
        }
        String storedState = stored.substring(0, separatorAt);
        String storedHash = stored.substring(separatorAt + 1);
        if (!storedHash.equals(requestHash)) {
            throw new IdempotencyKeyReusedException(messages.get(MessageKey.SALE_IDEMPOTENCY_KEY_REUSED));
        }
        if (IN_PROGRESS.equals(storedState)) {
            throw new DuplicateRequestException(messages.get(MessageKey.SALE_REQUEST_IN_PROGRESS));
        }
        return saleService.getById(Long.valueOf(storedState));
    }

    static String fingerprint(SaleRequest request) {
        String canonical = request.customerId() + SEPARATOR + request.bookId() + SEPARATOR + request.salesQuantity();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void remember(String redisKey, String value) {
        try {
            redisTemplate.opsForValue().set(redisKey, value, RESULT_LIFETIME);
        } catch (DataAccessException exception) {
            log.warn("Sale was recorded but its idempotency key could not be saved: {}", exception.getMessage());
        }
    }

    private void forget(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (DataAccessException exception) {
            log.warn("Could not release idempotency key {}: {}", redisKey, exception.getMessage());
        }
    }
}
