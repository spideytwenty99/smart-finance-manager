package com.saurabh.financemanager.report.dto;

import com.saurabh.financemanager.report.BudgetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record SpentPerCategory(
        @Schema(description = "ID of the category", example = "0557e90c-7bc4-40b1-a60d-c25ef6896fa3")
        UUID categoryId,

        @Schema(description = "Name of the category", example = "Food")
        String categoryName,

        @Schema(description = "Amount spent in this category this month", example = "250.00")
        BigDecimal totalSpent,

        @Schema(description = "Budget limit for the month; empty when there is no budget", example = "300.00")
        BigDecimal budgetLimit,

        @Schema(description = "Budget left; negative when over the limit, empty when there is no budget", example = "50.00")
        BigDecimal balance,

        @Schema(description = "Share of the budget used, in percent; empty when there is no budget", example = "83.33")
        BigDecimal budgetUsedInPercentage,

        @Schema(description = "OK below 80%, WARNING from 80% to 100%, OVER_LIMIT above 100%, NOT_SET without a budget",
                example = "WARNING")
        BudgetStatus budgetStatus
) {}
