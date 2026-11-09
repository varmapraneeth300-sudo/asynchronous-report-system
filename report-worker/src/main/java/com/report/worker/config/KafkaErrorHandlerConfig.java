
package com.report.worker.config;

import com.report.worker.service.ProcessingTokenRegistry;
import com.report.worker.service.ReportJobStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaErrorHandlerConfig {

    private final ReportJobStatusService statusService;
    private final ProcessingTokenRegistry tokenRegistry;

    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {

        FixedBackOff backOff = new FixedBackOff(5000L, 2L);

        DeadLetterPublishingRecoverer dltRecoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        "report-generation-dlt",
                                        record.partition()
                                )
                );

        dltRecoverer.setFailIfSendResultIsError(true);

        return new DefaultErrorHandler((record, exception) -> {

            String trackingId = (String) record.key();
            String messageId = getMessageId(record);

            Throwable rootCause =
                    NestedExceptionUtils.getMostSpecificCause(exception);

            String errorMessage = rootCause.getMessage();

            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = rootCause.getClass().getSimpleName();
            }

            if (errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }

            // Publish the original message to DLT.
            dltRecoverer.accept(record, exception);

            // Retrieve the token belonging to this processing attempt.
            String processingToken = tokenRegistry.get(messageId);

            if (processingToken == null) {
                log.warn(
                        "No processing token found. Cannot mark job FAILED: {}",
                        trackingId
                );
                return;
            }

            // Mark FAILED only if this attempt still owns the job.
            int updatedRows = statusService.markAsFailed(
                    trackingId,
                    errorMessage,
                    processingToken
            );

            if (updatedRows == 0) {
                log.info(
                        "Skipping FAILED update because ownership changed: {}",
                        trackingId
                );
            } else {
                log.error(
                        "Report job marked FAILED after retries: {}",
                        trackingId
                );
            }

            tokenRegistry.remove(messageId);

        }, backOff);
    }

    private String getMessageId(ConsumerRecord<?, ?> record) {
        return record.topic()
                + ":" + record.partition()
                + ":" + record.offset();
    }
}