package com.etiqa.sales.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventSenderTest {

    private record Ping(Long id, String note) {
    }

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaEventSender sender;

    @BeforeEach
    void setUp() {
        sender = new KafkaEventSender(new ObjectMapper(), kafkaTemplate);
    }

    @Test
    void sendsEventAsJsonWithTheGivenKey() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        sender.send("some.topic", "42", new Ping(42L, "hello"));

        verify(kafkaTemplate).send("some.topic", "42", "{\"id\":42,\"note\":\"hello\"}");
    }

    @Test
    void brokerRejection_onlyLogs() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        assertThatCode(() -> sender.send("some.topic", "42", new Ping(42L, "hello"))).doesNotThrowAnyException();
    }

    @Test
    void sendError_onlyLogs() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("closed"));

        assertThatCode(() -> sender.send("some.topic", "42", new Ping(42L, "hello"))).doesNotThrowAnyException();
    }
}
