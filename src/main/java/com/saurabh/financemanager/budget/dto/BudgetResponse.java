package com.saurabh.financemanager.budget.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
public class BudgetResponse {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private YearMonth month;
    private BigDecimal amountLimit;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
