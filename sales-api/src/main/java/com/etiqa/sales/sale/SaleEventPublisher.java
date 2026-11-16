package com.etiqa.sales.sale;

import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.common.messaging.KafkaEventSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SaleEventPublisher {

    private final KafkaEventSender kafkaEventSender;
    private final AppProperties appProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(SaleRecordedEvent event) {
        kafkaEventSender.send(appProperties.topics().sales(), event.saleId().toString(), event);
    }
}
