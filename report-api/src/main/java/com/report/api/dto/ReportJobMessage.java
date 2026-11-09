package com.report.api.dto;

import java.time.LocalDate;

public record ReportJobMessage(
        String trackingId,
        Long userId,
        String reportType,
        String format,
        LocalDate startDate,
        LocalDate endDate
) {}