
package com.report.api.dto;

import com.report.api.entity.Transaction;
import com.report.api.entity.enums.PaymentMethod;
import com.report.api.entity.enums.TransactionStatus;
import com.report.api.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String transactionReference,
        String username,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String category,
        String merchantName,
        PaymentMethod paymentMethod,
        String city,
        LocalDateTime transactionDate
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getUser().getUsername(),
                transaction.getAmount(),
                transaction.getFee(),
                transaction.getTaxAmount(),
                transaction.getTotalAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCategory(),
                transaction.getMerchantName(),
                transaction.getPaymentMethod(),
                transaction.getCity(),
                transaction.getTransactionDate()
        );
    }
}
