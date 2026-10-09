package com.saurabh.financemanager.transaction.dto;

import com.saurabh.financemanager.transaction.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Amount is requried")
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most two decimal places")
    private BigDecimal amount;


    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotNull(message = "Transaction Date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;
}
