package com.report.api.repo;

import com.report.api.entity.Transaction;
import com.report.api.entity.enums.TransactionStatus;
import com.report.api.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    @Query("""
    SELECT t FROM Transaction t
    WHERE (:userId IS NULL OR t.user.id = :userId)
    AND (:status IS NULL OR t.status = :status)
    AND (:type IS NULL OR t.type = :type)
    AND (:category IS NULL OR t.category = :category)
    AND (:startDate IS NULL OR t.transactionDate >= :startDate)
    AND (:endDate IS NULL OR t.transactionDate <= :endDate)
""")
    Page<Transaction> findTransactions(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    @Query("""
    SELECT t FROM Transaction t
    WHERE t.user.id = :userId
    AND t.transactionDate >= :startDate
    AND t.transactionDate < :endDate
    ORDER BY t.transactionDate ASC
""")
    List<Transaction> findTransactionsForReport(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("""
    SELECT t FROM Transaction t
    ORDER BY t.id ASC
""")
    List<Transaction> findTransactionsForHeavyReport(Pageable pageable);
}