package com.report.worker.service;

import com.report.worker.exception.ProcessingOwnershipLostException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportJobStatusService {

    private final JdbcTemplate jdbcTemplate;

    private static final int LEASE_DURATION_MINUTES = 5;

    public void markAsProcessing(String trackingId) {

        String sql = """
                UPDATE report_jobs
                SET status = ?, started_at = ?
                WHERE tracking_id = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                "PROCESSING",
                LocalDateTime.now(),
                trackingId
        );

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "Report job not found: " + trackingId
            );
        }
    }

    public int markAsCompleted(
            String trackingId,
            String filePath,
            String processingToken
    ) {
        String sql = """
            UPDATE report_jobs
            SET status = 'COMPLETED',
                file_path = ?,
                completed_at = NOW(),
                lease_expires_at = NULL
            WHERE tracking_id = ?
              AND processing_token = ?
              AND status = 'PROCESSING'
            """;

        int updatedRows = jdbcTemplate.update(
                sql,
                filePath,
                trackingId,
                processingToken
        );

        if (updatedRows == 0) {
            throw new ProcessingOwnershipLostException(trackingId);
        }

        return updatedRows;
    }


    public int markAsFailed(
            String trackingId,
            String errorMessage,
            String processingToken
    ) {
        String sql = """
                UPDATE report_jobs
                SET status = 'FAILED',
                    error_message = ?,
                    lease_expires_at = NULL,
                    processing_token = NULL,
                    processing_message_id = NULL
                WHERE tracking_id = ?
                  AND processing_token = ?
                  AND status = 'PROCESSING'
                """;

        return jdbcTemplate.update(
                sql,
                errorMessage,
                trackingId,
                processingToken
        );
    }


    public boolean isCompleted(String trackingId) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM report_jobs
                    WHERE tracking_id = ?
                      AND status = 'COMPLETED'
                )
                """;

        Boolean completed = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                trackingId
        );

        return Boolean.TRUE.equals(completed);
    }

    public boolean claimJob(String trackingId, String processingToken) {

        String sql = """
                UPDATE report_jobs
                SET status = 'PROCESSING',
                    started_at = NOW(),
                    lease_expires_at = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
                    processing_token = ?
                WHERE tracking_id = ?
                  AND status = 'PENDING'
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                processingToken,
                trackingId
        );

        return updatedRows == 1;
    }


    @Transactional
    public String claimOrResumeJob(String trackingId, String messageId) {

        String newToken = UUID.randomUUID().toString();

        String sql = """
            UPDATE report_jobs
            SET
                processing_token = ?,
                processing_message_id = ?,
                lease_expires_at = DATE_ADD(NOW(), INTERVAL ? MINUTE),
                started_at = COALESCE(started_at, NOW()),
                status = 'PROCESSING'
            WHERE tracking_id = ?
              AND (
                    status = 'PENDING'
                    OR (
                        status = 'PROCESSING'
                        AND lease_expires_at <= NOW()
                    )
                    OR (
                        status = 'PROCESSING'
                        AND processing_message_id = ?
                    )
              )
            """;

        int updatedRows = jdbcTemplate.update(
                sql,
                newToken,
                messageId,
                LEASE_DURATION_MINUTES,
                trackingId,
                messageId
        );

        if (updatedRows == 0) {
            return null;
        }

        return newToken;
    }


    public int renewLease(String trackingId, String processingToken) {

        String sql = """
            UPDATE report_jobs
            SET lease_expires_at = DATE_ADD(NOW(), INTERVAL ? MINUTE)
            WHERE tracking_id = ?
              AND processing_token = ?
              AND status = 'PROCESSING'
            """;

        return jdbcTemplate.update(
                sql,
                LEASE_DURATION_MINUTES,
                trackingId,
                processingToken
        );
    }

    public boolean isCurrentOwner(String trackingId, String processingToken) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM report_jobs
                WHERE tracking_id = ?
                  AND processing_token = ?
                  AND status = 'PROCESSING'
            )
            """;

        Boolean result = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                trackingId,
                processingToken
        );

        return Boolean.TRUE.equals(result);
    }

}