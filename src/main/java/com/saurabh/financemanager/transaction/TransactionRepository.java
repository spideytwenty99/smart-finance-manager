package com.saurabh.financemanager.transaction;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByCategoryIdAndTransactionDateBetween(
            UUID id,
            LocalDate startDate,
            LocalDate endDate

    );

    long countByCategoryId(UUID categoryId);


    List<Transaction> findByCategoryId(UUID categoryId);
}
