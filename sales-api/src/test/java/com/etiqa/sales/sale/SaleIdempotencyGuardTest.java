package com.etiqa.sales.sale;

import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.DuplicateRequestException;
import com.etiqa.sales.common.exception.IdempotencyKeyReusedException;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleIdempotencyGuardTest {

    private static final String ALICE_KEY = "idemp:alice:key-1";
    private static final Duration DAY = SaleIdempotencyGuard.RESULT_LIFETIME;
    private static final Duration LOCK = SaleIdempotencyGuard.IN_PROGRESS_LIFETIME;
    private static final SaleRequest REQUEST = new SaleRequest(1L, 10L, 1);
    private static final String REQUEST_HASH = SaleIdempotencyGuard.fingerprint(REQUEST);
    private static final SaleResponse SALE = new SaleResponse(42L, Instant.parse("2026-11-16T01:00:00Z"), 10L,
            "Effective Java", 1L, "Alice Tan", 1, new BigDecimal("45.50"), new BigDecimal("45.50"));

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> redisValues;

    @Mock
    private SaleService saleService;

    private SaleIdempotencyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SaleIdempotencyGuard(redisTemplate, saleService, TestMessages.create());
    }

    @Test
    void withoutKey_justRecordsTheSale() {
        when(saleService.recordSale(REQUEST)).thenReturn(SALE);

        assertThat(guard.recordOnce("alice", null, REQUEST)).isEqualTo(SALE);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void firstUse_recordsAndRemembersSaleWithRequestHash() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(ALICE_KEY, "in-progress|" + REQUEST_HASH, LOCK)).thenReturn(true);
        when(saleService.recordSale(REQUEST)).thenReturn(SALE);

        assertThat(guard.recordOnce("alice", "key-1", REQUEST)).isEqualTo(SALE);
        verify(redisValues).set(ALICE_KEY, "42|" + REQUEST_HASH, DAY);
    }

    @Test
    void sameKeySameRequest_returnsTheOriginalSale() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisValues.get(ALICE_KEY)).thenReturn("42|" + REQUEST_HASH);
        when(saleService.getById(42L)).thenReturn(SALE);

        assertThat(guard.recordOnce("alice", "key-1", REQUEST)).isEqualTo(SALE);
        verify(saleService, never()).recordSale(any());
    }

    @Test
    void sameKeyDifferentRequest_isRejected() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisValues.get(ALICE_KEY)).thenReturn("42|" + REQUEST_HASH);

        assertThatThrownBy(() -> guard.recordOnce("alice", "key-1", new SaleRequest(1L, 10L, 5)))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        verify(saleService, never()).getById(any());
    }

    @Test
    void sameKeyWhileFirstIsRunning_isRejectedAsDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisValues.get(ALICE_KEY)).thenReturn("in-progress|" + REQUEST_HASH);

        assertThatThrownBy(() -> guard.recordOnce("alice", "key-1", REQUEST)).isInstanceOf(DuplicateRequestException.class);
    }

    @Test
    void lockIsShortButTheResultIsKeptForADay() {
        assertThat(LOCK).isEqualTo(Duration.ofMinutes(2));
        assertThat(DAY).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void storedValueWithoutSeparator_isTreatedAsDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisValues.get(ALICE_KEY)).thenReturn("42");

        assertThatThrownBy(() -> guard.recordOnce("alice", "key-1", REQUEST)).isInstanceOf(DuplicateRequestException.class);
        verify(saleService, never()).getById(any());
    }

    @Test
    void keysAreKeptPerUser() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(eq("idemp:bob:key-1"), anyString(), any(Duration.class))).thenReturn(true);
        when(saleService.recordSale(REQUEST)).thenReturn(SALE);

        guard.recordOnce("bob", "key-1", REQUEST);

        verify(redisValues, never()).get(ALICE_KEY);
        verify(redisValues).set("idemp:bob:key-1", "42|" + REQUEST_HASH, DAY);
    }

    @Test
    void failedSale_releasesTheKey() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(saleService.recordSale(REQUEST)).thenThrow(new BusinessValidationException("out of stock"));

        assertThatThrownBy(() -> guard.recordOnce("alice", "key-1", REQUEST)).isInstanceOf(BusinessValidationException.class);
        verify(redisTemplate).delete(ALICE_KEY);
    }

    @Test
    void redisDown_stillRecordsTheSale() {
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        when(saleService.recordSale(REQUEST)).thenReturn(SALE);

        assertThat(guard.recordOnce("alice", "key-1", REQUEST)).isEqualTo(SALE);
    }

    @Test
    void overlongKey_isRejected() {
        assertThatThrownBy(() -> guard.recordOnce("alice", "k".repeat(101), REQUEST))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Idempotency-Key must be at most 100 characters.");
    }
}
