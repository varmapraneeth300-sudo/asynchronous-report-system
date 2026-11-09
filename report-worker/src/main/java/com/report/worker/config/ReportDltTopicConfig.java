package com.report.worker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ReportDltTopicConfig {

    @Bean
    public NewTopic reportGenerationDltTopic() {
        return TopicBuilder.name("report-generation-dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }
}