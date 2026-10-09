package com.saurabh.financemanager.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    //All budgets for one month
    List<Budget> findByMonth(LocalDate month);

    //One category's budget for one month
    Budget findByCategoryIdAndMonth(UUID categoryId, LocalDate month);

    //Duplicate check before creating a budget
    boolean existsByCategoryIdAndMonth(UUID categoryId, LocalDate month);

    //for delete check: is this category used by any budget?
    long countByCategoryId(UUID id);



}
