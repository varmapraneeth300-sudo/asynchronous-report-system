package com.report.worker.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ReportDltConsumer {

    @KafkaListener(
            topics = "report-generation-dlt",
            groupId = "report-dlt-inspector-group"
    )
    public void consume(ConsumerRecord<String, String> record) {

        log.error("Failed report message received in DLT");

        // Original message key
        log.error("Tracking ID: {}", record.key());

        // Original JSON message
        log.error("Original message: {}", record.value());

        // Failure-related Kafka headers
        for (Header header : record.headers()) {

            String headerValue =
                    new String(
                            header.value(),
                            StandardCharsets.UTF_8
                    );

            log.error("{}: {}", header.key(), headerValue);
        }
    }
}