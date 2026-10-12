package com.saurabh.financemanager.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
public class BudgetResponse {

    @Schema(description = "ID of the budget", example = "a3c1e7d2-5b4f-4c8e-9a1d-2f6b7c8d9e0f",
            accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "ID of the budget's category", example = "55d802f4-e571-4819-b89a-1f4edecedb9c")
    private UUID categoryId;

    @Schema(description = "Name of the budget's category", example = "Groceries")
    private String categoryName;

    @Schema(description = "Month of the budget", type = "string", example = "2026-10")
    private YearMonth month;

    @Schema(description = "Spending limit in euros", example = "300.00")
    private BigDecimal amountLimit;

    @Schema(description = "When the budget was created", type = "string", example = "2026-10-09T09:56:00")
    private LocalDateTime createdAt;

    @Schema(description = "When the budget was last changed", type = "string", example = "2026-10-09T10:03:00")
    private LocalDateTime updatedAt;
}
