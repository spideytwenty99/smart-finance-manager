package com.saurabh.financemanager.budget.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BudgetUpdateRequest {

    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount limit can have at most two decimal places")
    private BigDecimal amountLimit;
}