package com.etiqa.sales.product;

import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.common.messaging.KafkaEventSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockEventPublisher {

    private final KafkaEventSender kafkaEventSender;
    private final AppProperties appProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(StockLevelChanged event) {
        kafkaEventSender.send(appProperties.topics().stock(), event.bookId().toString(), event);
    }
}
