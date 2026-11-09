package com.report.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record AsyncReportRequest(

        @NotNull
        Long userId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate

) {

    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null
                || !startDate.isAfter(endDate);
    }
}