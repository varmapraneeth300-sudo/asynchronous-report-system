package com.report.api.controller;

import com.report.api.dto.AsyncReportRequest;
import com.report.api.dto.ReportAcknowledgement;
import com.report.api.dto.ReportRequest;
import com.report.api.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/transaction")
    public ResponseEntity<byte[]> generateTransactionReport(
            @Valid @RequestBody ReportRequest request) {
        long startTime = System.currentTimeMillis();

        String csv = reportService.generateTransactionReport(request);

        long endTime = System.currentTimeMillis();

        System.out.println(
                "Report generation time: " +
                        (endTime - startTime) + " ms"
        );

        byte[] file = csv.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=transaction-report.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(file.length)
                .body(file);
    }

    @GetMapping("/transaction/heavy")
    public ResponseEntity<byte[]> generateHeavyTransactionReport(
            @RequestParam int recordCount) {

        long startTime = System.currentTimeMillis();

        String csv =
                reportService.generateHeavyTransactionReport(recordCount);

        byte[] file = csv.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        );

        long endTime = System.currentTimeMillis();

        System.out.println(
                "Total report time: "
                        + (endTime - startTime) + " ms"
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=heavy-transaction-report.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(file.length)
                .body(file);
    }

    @PostMapping("/transaction/async")
    public ResponseEntity<ReportAcknowledgement> requestAsyncReport(
            @Valid @RequestBody AsyncReportRequest request) {

        ReportAcknowledgement acknowledgement =
                reportService.createAsyncReportJob(request);

        return ResponseEntity.accepted().body(acknowledgement);
    }
}
