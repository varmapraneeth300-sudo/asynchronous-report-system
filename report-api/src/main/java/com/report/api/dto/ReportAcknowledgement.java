package com.report.api.dto;

import java.util.UUID;

public record ReportAcknowledgement(
        String trackingId,
        String status,
        String message
) {
}