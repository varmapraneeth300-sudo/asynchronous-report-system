package com.report.api.service;

import com.report.api.dto.ReportJobMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReportEventProducer {

    private static final String TOPIC = "report-generation";
    private final ObjectMapper objectMapper;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishReportJob(ReportJobMessage message) {
        String json = objectMapper.writeValueAsString(message);

        kafkaTemplate.send(
                TOPIC,
                message.trackingId(),
                json
        );
    }
}