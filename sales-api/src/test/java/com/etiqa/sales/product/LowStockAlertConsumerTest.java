package com.etiqa.sales.product;

import com.etiqa.sales.support.TestAppProperties;
import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LowStockAlertConsumerTest {

    private static final int THRESHOLD = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuditLogService auditLogService;

    private LowStockAlertConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LowStockAlertConsumer(objectMapper, TestAppProperties.withDefaults(), auditLogService);
    }

    @Test
    void alertsWhenStockCrossesThreshold() throws JsonProcessingException {
        StockLevelChanged change = stockChange(12, 9);

        consumer.handleStockLevelChanged(json(change));

        verify(auditLogService).record(AuditedResource.PRODUCT, AuditAction.LOW_STOCK_ALERT, 10L, change);
    }

    @Test
    void staysQuietWhenStockWasAlreadyLow() throws JsonProcessingException {
        consumer.handleStockLevelChanged(json(stockChange(9, 8)));
        consumer.handleStockLevelChanged(json(stockChange(8, 3)));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void alertsAgainAfterARestockLiftsStockBackUp() throws JsonProcessingException {
        consumer.handleStockLevelChanged(json(stockChange(11, 9)));
        consumer.handleStockLevelChanged(json(stockChange(9, 7)));
        consumer.handleStockLevelChanged(json(stockChange(7, 27)));
        consumer.handleStockLevelChanged(json(stockChange(27, 5)));

        verify(auditLogService, times(2)).record(eq(AuditedResource.PRODUCT), eq(AuditAction.LOW_STOCK_ALERT), eq(10L), any());
    }

    @Test
    void ignoresStockThatStaysAtOrAboveThreshold() throws JsonProcessingException {
        consumer.handleStockLevelChanged(json(stockChange(15, THRESHOLD)));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void malformedPayloadIsLeftForTheDeadLetterTopic() {
        assertThatThrownBy(() -> consumer.handleStockLevelChanged("not-json"))
                .isInstanceOf(JsonProcessingException.class);

        verifyNoInteractions(auditLogService);
    }

    private static StockLevelChanged stockChange(int stockBefore, int stockAfter) {
        return new StockLevelChanged(10L, "Effective Java", stockBefore, stockAfter);
    }

    private String json(StockLevelChanged change) throws JsonProcessingException {
        return objectMapper.writeValueAsString(change);
    }
}
