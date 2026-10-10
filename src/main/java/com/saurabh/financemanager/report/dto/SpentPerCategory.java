package com.saurabh.financemanager.report.dto;

import com.saurabh.financemanager.report.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SpentPerCategory(
        UUID categoryId,
        String categoryName,
        BigDecimal totalSpent,
        BigDecimal budgetLimit,
        BigDecimal balance,
        BigDecimal budgetUsedInPercentage,
        BudgetStatus budgetStatus
) { }
