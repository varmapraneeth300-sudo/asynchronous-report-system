package com.report.worker.service;

import com.report.worker.dto.ReportJobMessage;
import com.report.worker.entity.Transaction;
import com.report.worker.exception.ProcessingOwnershipLostException;
import com.report.worker.repo.TransactionRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private static final int BATCH_SIZE = 5000;

    private final TransactionRepo transactionRepo;
    private final ReportJobStatusService statusService;

    public String generateTransactionReport(
            ReportJobMessage reportJob,
            String processingToken
    ) {

        Path reportDirectory = Path.of("reports");

        String fileName = reportJob.trackingId() + ".csv";

        Path filePath = reportDirectory.resolve(fileName);

        LocalDateTime startDate =
                reportJob.startDate().atStartOfDay();

        LocalDateTime endDate =
                reportJob.endDate().plusDays(1).atStartOfDay();

        long lastId = 0L;

        boolean reportGenerated = false;

        try {

            Files.createDirectories(reportDirectory);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8
            )) {

                writeHeader(writer);

                while (true) {

                    if (!statusService.isCurrentOwner(
                            reportJob.trackingId(),
                            processingToken
                    )) {
                        throw new ProcessingOwnershipLostException(
                                reportJob.trackingId()
                        );
                    }

                    List<Transaction> batch =
                            transactionRepo.findNextBatch(
                                    reportJob.userId(),
                                    startDate,
                                    endDate,
                                    lastId,
                                    PageRequest.of(0, BATCH_SIZE)
                            );

                    if (batch.isEmpty()) {
                        break;
                    }

                    for (Transaction transaction : batch) {
                        writeTransaction(writer, transaction);
                    }

                    lastId = batch.get(batch.size() - 1).getId();

                    writer.flush();
                }
            }

            // Reaching here means the writer was closed successfully.
            reportGenerated = true;

            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {

            throw new UncheckedIOException(
                    "Failed to generate transaction report", e
            );

        } finally {

            if (!reportGenerated) {

                try {
                    Files.deleteIfExists(filePath);

                    log.info("Incomplete report file cleaned up: {}", filePath);

                } catch (IOException cleanupException) {

                    log.error(
                            "Failed to delete incomplete report file: {}",
                            filePath,
                            cleanupException
                    );
                }
            }
        }
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write(
                "ID,Transaction Reference,User ID,Amount,Currency," +
                        "Transaction Type,Transaction Status,Transaction Date,Merchant Name"
        );
        writer.newLine();
    }

    private void writeTransaction(
            BufferedWriter writer,
            Transaction transaction
    ) throws IOException {

        writer.write(String.join(",",
                csv(transaction.getId()),
                csv(transaction.getTransactionReference()),
                csv(transaction.getUserId()),
                csv(transaction.getAmount()),
                csv(transaction.getCurrency()),
                csv(transaction.getTransactionType()),
                csv(transaction.getTransactionStatus()),
                csv(transaction.getTransactionDate()),
                csv(transaction.getMerchantName())
        ));

        writer.newLine();
    }

    private String csv(Object value) {

        if (value == null) {
            return "";
        }

        String text = value.toString();

        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void simulateDatabaseFailure() {
        throw new RuntimeException("Simulated DLT test failure");
    }
}