package com.etiqa.sales.product;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertConsumer {

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final AuditLogService auditLogService;

    @KafkaListener(topics = "${app.topics.stock}")
    public void handleStockLevelChanged(String eventJson) throws JsonProcessingException {
        StockLevelChanged event = objectMapper.readValue(eventJson, StockLevelChanged.class);
        if (!droppedBelowThreshold(event)) {
            return;
        }
        log.warn(
                "Low stock alert for '{}' (bookId={}): {} copies left",
                event.bookTitle(),
                event.bookId(),
                event.remainingStock()
        );
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.LOW_STOCK_ALERT, event.bookId(), event);
    }

    private boolean droppedBelowThreshold(StockLevelChanged event) {
        int threshold = appProperties.lowStockThreshold();
        return event.previousStock() >= threshold && event.remainingStock() < threshold;
    }
}
