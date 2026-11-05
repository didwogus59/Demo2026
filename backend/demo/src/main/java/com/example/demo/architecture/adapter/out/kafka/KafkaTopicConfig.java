package com.example.demo.architecture.adapter.out.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic couponIssueEventsTopic() {
        return TopicBuilder.name(KafkaCouponEventAdapter.TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

