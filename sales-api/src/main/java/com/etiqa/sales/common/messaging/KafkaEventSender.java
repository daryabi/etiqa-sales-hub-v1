package com.etiqa.sales.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventSender {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Only called after commit. A failed send is logged and dropped on purpose: Kafka being down
    // must never fail a checkout. An outbox table would be the fix if we ever need guaranteed delivery.
    public void send(String topic, String key, Object event) {
        String eventName = event.getClass().getSimpleName();
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((sendResult, sendFailure) -> {
                        if (sendFailure != null) {
                            log.error("Kafka did not accept {} with key {}", eventName, key, sendFailure);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException exception) {
            log.error("Failed to publish {} with key {} to Kafka", eventName, key, exception);
        }
    }
}
