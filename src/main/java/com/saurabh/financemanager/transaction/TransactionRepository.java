package com.saurabh.financemanager.transaction;


import com.saurabh.financemanager.report.TotalByCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("""
            SELECT new com.saurabh.financemanager.report.TotalByCategory(
            t.category.id, t.category.name, SUM(t.amount))
            FROM Transaction t
            WHERE t.transactionDate BETWEEN :start AND :end
            AND t.category.transactionType = :type
            GROUP BY t.category.id, t.category.name
            """)
    List<TotalByCategory> totalPerCategory(@Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           @Param("type") TransactionType type);


    @Query("""
            SELECT COALESCE(SUM(t.amount),0)
            FROM Transaction t
            WHERE t.transactionDate BETWEEN :start AND :end
            AND t.category.transactionType= :type
            """)
    BigDecimal sumByType(@Param("start") LocalDate start,
                         @Param("end") LocalDate end,
                         @Param("type") TransactionType type);
}
