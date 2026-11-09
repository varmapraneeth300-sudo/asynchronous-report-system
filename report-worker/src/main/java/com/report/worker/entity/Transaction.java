package com.report.worker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    private Long id;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "user_id")
    private Long userId;

    private BigDecimal amount;

    private String currency;

    @Column(name = "type")
    private String transactionType;

    @Column(name = "status")
    private String transactionStatus;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "merchant_name")
    private String merchantName;
}