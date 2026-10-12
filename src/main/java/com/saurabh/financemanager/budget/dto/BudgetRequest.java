package com.saurabh.financemanager.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BudgetRequest {

    @Schema(description = "ID of an expense category")
    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @Schema(description = "Month of the budget in the format YYYY-MM", type = "string", example = "2026-10")
    @NotNull(message = "Month is required")
    private YearMonth month;

    @Schema(description = "Spending limit in euros for this category and month", example = "300.00")
    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount limit can have at most two decimal places")
    private BigDecimal amountLimit;

}
