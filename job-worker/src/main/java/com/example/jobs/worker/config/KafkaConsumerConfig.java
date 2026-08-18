package com.example.jobs.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic deadLetterTopic(
        @Value("${app.kafka.dead-letter-topic}") String topicName
    ) {
        return TopicBuilder.name(topicName)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
        KafkaTemplate<Object, Object> kafkaTemplate,
        @Value("${app.kafka.dead-letter-topic}") String deadLetterTopic
    ) {
        DeadLetterPublishingRecoverer recoverer = 
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                    deadLetterTopic, 
                    record.partition()
                )
            );
        
        FixedBackOff backOff = new FixedBackOff(
            2_000L,
            2L
        );

        return new DefaultErrorHandler(
            recoverer,
            backOff
        );
    }
}
