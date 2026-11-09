package com.report.api.controller;

import com.report.api.dto.TransactionResponse;
import com.report.api.entity.Transaction;
import com.report.api.entity.enums.TransactionStatus;
import com.report.api.entity.enums.TransactionType;
import com.report.api.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepo transactionRepository;

    @GetMapping
    public Page<TransactionResponse> getAllTransactions(@PageableDefault(size = 10) Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionResponse::from);
    }

    @GetMapping("/filter")
    public Page<TransactionResponse> filterTransactions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        return transactionRepository.findTransactions(
                userId,
                status,
                type,
                category,
                startDate,
                endDate,
                pageable
        ).map(TransactionResponse::from);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(@PathVariable Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Transaction not found with id: " + id
                        )
                );

        return TransactionResponse.from(transaction);
    }

    @GetMapping("/user/{userId}")
    public Page<TransactionResponse> getUserTransactions(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return transactionRepository.findByUserId(userId, pageable)
                .map(TransactionResponse::from);
    }
}
