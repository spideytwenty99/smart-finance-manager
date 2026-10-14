package com.saurabh.financemanager.transaction;


import com.saurabh.financemanager.report.TotalByCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID> {


    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findByUserId(UUID userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndCategoryId(UUID userId, UUID categoryId);

    List<Transaction> findByUserIdAndCategoryIdAndTransactionDateBetween(
            UUID userId, UUID categoryId, LocalDate startDate, LocalDate endDate);

    long countByCategoryId(UUID categoryId);


    @Query("""
            SELECT new com.saurabh.financemanager.report.TotalByCategory(
                t.category.id, t.category.name, SUM(t.amount))
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :start AND :end
              AND t.category.transactionType = :type
            GROUP BY t.category.id, t.category.name
            """)
    List<TotalByCategory> totalPerCategory(@Param("userId") UUID userId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           @Param("type") TransactionType type);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.transactionDate BETWEEN :start AND :end
              AND t.category.transactionType = :type
            """)
    BigDecimal sumByType(@Param("userId") UUID userId,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end,
                         @Param("type") TransactionType type);

}