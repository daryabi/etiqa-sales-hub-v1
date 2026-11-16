package com.etiqa.sales.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    private static final int TOPIC_PARTITIONS = 3;
    private static final int TOPIC_REPLICAS = 1;
    private static final long RETRY_INTERVAL_MS = 1_000L;
    private static final long MAX_RETRIES = 2L;

    @Bean
    public NewTopic salesTopic(AppProperties appProperties) {
        return topic(appProperties.topics().sales());
    }

    @Bean
    public NewTopic stockTopic(AppProperties appProperties) {
        return topic(appProperties.topics().stock());
    }

    @Bean
    public NewTopic stockDeadLetterTopic(AppProperties appProperties) {
        return topic(appProperties.topics().stock() + DEAD_LETTER_SUFFIX);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate, (failedRecord, failure) ->
                        new TopicPartition(failedRecord.topic() + DEAD_LETTER_SUFFIX, failedRecord.partition())),
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES)
        );
        errorHandler.addNotRetryableExceptions(JsonProcessingException.class);
        return errorHandler;
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .build();
    }
}
