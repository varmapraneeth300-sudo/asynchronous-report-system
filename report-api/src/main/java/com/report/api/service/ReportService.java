package com.report.api.service;

import com.report.api.dto.AsyncReportRequest;
import com.report.api.dto.ReportAcknowledgement;
import com.report.api.dto.ReportJobMessage;
import com.report.api.dto.ReportRequest;
import com.report.api.entity.ReportJob;
import com.report.api.entity.Transaction;
import com.report.api.entity.User;
import com.report.api.entity.enums.ReportFormat;
import com.report.api.entity.enums.ReportStatus;
import com.report.api.entity.enums.ReportType;
import com.report.api.repo.ReportJobRepo;
import com.report.api.repo.TransactionRepo;
import com.report.api.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final TransactionRepo transactionRepo;
    private final UserRepo userRepository;
    private final ReportJobRepo reportJobRepo;
    private final ReportEventProducer reportEventProducer;

    @Transactional(readOnly = true)
    public String generateTransactionReport(ReportRequest request) {
        // Validate user
        User user = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        LocalDateTime startDate =
                request.startDate().atStartOfDay();

        LocalDateTime endDate =
                request.endDate().plusDays(1).atStartOfDay();

        // Fetch matching transactions
        List<Transaction> transactions =
                transactionRepo.findTransactionsForReport(
                        user.getId(),
                        startDate,
                        endDate
                );

        StringBuilder csv = new StringBuilder();
        csv.append("Transaction Reference,")
                .append("Amount,")
                .append("Currency,")
                .append("Type,")
                .append("Status,")
                .append("Category,")
                .append("Merchant,")
                .append("Payment Method,")
                .append("Transaction Date\n");

        for (Transaction transaction : transactions) {

            csv.append(transaction.getTransactionReference()).append(",")
                    .append(transaction.getAmount()).append(",")
                    .append(transaction.getCurrency()).append(",")
                    .append(transaction.getType()).append(",")
                    .append(transaction.getStatus()).append(",")
                    .append(transaction.getCategory()).append(",")
                    .append(transaction.getMerchantName()).append(",")
                    .append(transaction.getPaymentMethod()).append(",")
                    .append(transaction.getTransactionDate())
                    .append("\n");
        }

        return csv.toString();
    }


    @Transactional(readOnly = true)
    public String generateHeavyTransactionReport(int recordCount) {

        long retrievalStart = System.currentTimeMillis();

        List<Transaction> transactions =
                transactionRepo.findTransactionsForHeavyReport(
                        PageRequest.of(
                                0,
                                recordCount,
                                Sort.by("id").ascending()
                        )
                );

        long retrievalEnd = System.currentTimeMillis();

        System.out.println(
                "Database retrieval time: "
                        + (retrievalEnd - retrievalStart) + " ms"
        );

        long csvStart = System.currentTimeMillis();

        StringBuilder csv = new StringBuilder();

        csv.append("Transaction Reference,")
                .append("Amount,")
                .append("Currency,")
                .append("Type,")
                .append("Status,")
                .append("Category,")
                .append("Merchant,")
                .append("Payment Method,")
                .append("Transaction Date\n");

        for (Transaction transaction : transactions) {
            csv.append(transaction.getTransactionReference()).append(",")
                    .append(transaction.getAmount()).append(",")
                    .append(transaction.getCurrency()).append(",")
                    .append(transaction.getType()).append(",")
                    .append(transaction.getStatus()).append(",")
                    .append(transaction.getCategory()).append(",")
                    .append(transaction.getMerchantName()).append(",")
                    .append(transaction.getPaymentMethod()).append(",")
                    .append(transaction.getTransactionDate())
                    .append("\n");
        }

        long csvEnd = System.currentTimeMillis();

        System.out.println(
                "CSV generation time: "
                        + (csvEnd - csvStart) + " ms"
        );

        System.out.println(
                "Records retrieved: " + transactions.size()
        );

        return csv.toString();
    }

    @Transactional
    public ReportAcknowledgement createAsyncReportJob(
            AsyncReportRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        ReportJob reportJob = new ReportJob();

        reportJob.setUser(user);
        reportJob.setReportType(ReportType.TRANSACTION);
        reportJob.setFormat(ReportFormat.CSV);
        reportJob.setStatus(ReportStatus.PENDING);
        reportJob.setStartDate(request.startDate());
        reportJob.setEndDate(request.endDate());

        ReportJob savedJob = reportJobRepo.save(reportJob);

        ReportJobMessage message = new ReportJobMessage(
                savedJob.getTrackingId(),
                user.getId(),
                savedJob.getReportType().name(),
                savedJob.getFormat().name(),
                savedJob.getStartDate(),
                savedJob.getEndDate()
        );

        reportEventProducer.publishReportJob(message);

        return new ReportAcknowledgement(
                savedJob.getTrackingId().toString(),
                savedJob.getStatus().name(),
                "Report generation request accepted"
        );
    }
}
