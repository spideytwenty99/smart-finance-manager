package com.saurabh.financemanager.transaction.dto;

import com.saurabh.financemanager.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Amount in euros, always positive; the category's type decides income or expense",
            example = "12.50")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most two decimal places")
    private BigDecimal amount;

    @Schema(description = "What the transaction was for; used for automatic categorization", example = "REWE Frankfurt Hbf")
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Schema(description = "Date of the transaction; can't be in the future", example = "2026-10-05")
    @NotNull(message = "Transaction Date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @Schema(description = "Optional. If given, this category is always used. Leave it out to categorize automatically.")
    private UUID categoryId;

    @Schema(description = "Required when no categoryId is given. If both are sent, it must match the category's type.",
            example = "EXPENSE")
    private TransactionType transactionType;
}
