package com.report.worker.repo;

import com.report.worker.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
              AND t.id > :lastId
            ORDER BY t.id ASC
            """)
    List<Transaction> findNextBatch(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}