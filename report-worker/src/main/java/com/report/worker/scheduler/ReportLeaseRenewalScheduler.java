package com.report.worker.scheduler;

import com.report.worker.service.ProcessingTokenRegistry;
import com.report.worker.service.ReportJobStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportLeaseRenewalScheduler {

    private final ProcessingTokenRegistry tokenRegistry;
    private final ReportJobStatusService statusService;

    @Scheduled(
            fixedDelay = 60_000,
            initialDelay = 60_000
    )
    public void renewActiveLeases() {

        for (Map.Entry<String, ProcessingTokenRegistry.Lease> entry
                : tokenRegistry.getActiveLeases()) {

            String messageId = entry.getKey();

            ProcessingTokenRegistry.Lease lease = entry.getValue();

            int updatedRows = statusService.renewLease(
                    lease.trackingId(),
                    lease.processingToken()
            );

            if (updatedRows == 0) {
                log.warn(
                        "Lease renewal failed. Ownership may have changed. Message ID: {}",
                        messageId
                );

                tokenRegistry.remove(messageId);
            } else {
                log.info(
                        "Lease renewed for tracking ID: {}",
                        lease.trackingId()
                );
            }
        }
    }
}
