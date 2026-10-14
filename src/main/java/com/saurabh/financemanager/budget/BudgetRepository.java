package com.saurabh.financemanager.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findByUserIdAndMonth(UUID userId, LocalDate month);

    boolean existsByCategoryIdAndMonth(UUID categoryId, LocalDate month);

    long countByCategoryId(UUID categoryId);



}
