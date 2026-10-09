package com.saurabh.financemanager.budget.dto;

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

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Month is required")
    private YearMonth month;

    @NotNull(message = "Amount limit must not be null")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2,message = "Amount can have at most two decimal places")
    private BigDecimal amountLimit;

}
