package com.etiqa.sales.product;

import com.etiqa.sales.support.TestAppProperties;
import com.etiqa.sales.common.messaging.KafkaEventSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockEventPublisherTest {

    @Mock
    private KafkaEventSender kafkaEventSender;

    @Test
    void sendsStockChangesKeyedByBookId() {
        StockEventPublisher publisher = new StockEventPublisher(kafkaEventSender, TestAppProperties.withDefaults());
        StockLevelChanged change = new StockLevelChanged(10L, "Effective Java", 12, 9);

        publisher.publishAfterCommit(change);

        verify(kafkaEventSender).send("inventory.stock-changed.v1", "10", change);
    }
}
