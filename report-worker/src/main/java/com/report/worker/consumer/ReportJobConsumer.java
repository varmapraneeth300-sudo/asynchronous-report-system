
package com.report.worker.consumer;

import com.report.worker.dto.ReportJobMessage;
import com.report.worker.exception.ProcessingOwnershipLostException;
import com.report.worker.service.ProcessingTokenRegistry;
import com.report.worker.service.ReportGenerationService;
import com.report.worker.service.ReportJobStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportJobConsumer {

    private final ObjectMapper objectMapper;
    private final ReportJobStatusService statusService;
    private final ReportGenerationService reportGenerationService;
    private final ProcessingTokenRegistry tokenRegistry;

    @KafkaListener(
            topics = "report-generation",
            groupId = "report-worker-group"
    )
    public void consume(ConsumerRecord<String, String> record)
            throws Exception {

        String message = record.value();

        ReportJobMessage reportJob =
                objectMapper.readValue(message, ReportJobMessage.class);

        String trackingId = reportJob.trackingId();

        String messageId = record.topic()
                + ":" + record.partition()
                + ":" + record.offset();

        log.info("Received report job: {}", trackingId);

        if (statusService.isCompleted(trackingId)) {
            log.info("Report already completed. Skipping: {}", trackingId);
            return;
        }

        String processingToken =
                statusService.claimOrResumeJob(trackingId, messageId);

        if (processingToken == null) {
            log.info("Could not claim report job: {}", trackingId);
            return;
        }

        // Store the token so the error handler can access it.
        tokenRegistry.store(messageId, trackingId, processingToken);

        try {
            String filePath = reportGenerationService.generateTransactionReport(
                    reportJob,
                    processingToken
            );

            statusService.markAsCompleted(
                    trackingId,
                    filePath,
                    processingToken
            );

            log.info(
                    "Report generation completed. Tracking ID: {}, File: {}",
                    trackingId,
                    filePath
            );

        } catch (ProcessingOwnershipLostException ex) {

            log.warn(
                    "Worker lost ownership. Stopping processing: {}",
                    trackingId
            );

            throw ex;

        } finally {
            tokenRegistry.remove(messageId);
        }
    }
}
