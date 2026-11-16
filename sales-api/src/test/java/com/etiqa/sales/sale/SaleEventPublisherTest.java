package com.etiqa.sales.sale;

import com.etiqa.sales.support.TestAppProperties;
import com.etiqa.sales.common.messaging.KafkaEventSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaleEventPublisherTest {

    @Mock
    private KafkaEventSender kafkaEventSender;

    @Test
    void sendsSaleEventsKeyedBySaleId() {
        SaleEventPublisher publisher = new SaleEventPublisher(kafkaEventSender, TestAppProperties.withDefaults());
        SaleRecordedEvent event = new SaleRecordedEvent(42L, 10L, 7L, 2, new BigDecimal("91.00"));

        publisher.publishAfterCommit(event);

        verify(kafkaEventSender).send("sales.recorded.v1", "42", event);
    }
}
