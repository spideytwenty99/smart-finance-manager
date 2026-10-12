package com.saurabh.financemanager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MonthlySummary(
        @Schema(description = "The month of the report", type = "string", example = "2026-08")
        YearMonth month,

        @Schema(description = "Sum of all income in the month", example = "2000.00")
        BigDecimal totalIncome,

        @Schema(description = "Sum of all expenses in the month", example = "1060.00")
        BigDecimal totalExpenses,

        @Schema(description = "Income minus expenses", example = "940.00")
        BigDecimal balance,

        @Schema(description = "One line per expense category, sorted by spending, highest first")
        List<SpentPerCategory> spentPerCategoriesList
) {}
