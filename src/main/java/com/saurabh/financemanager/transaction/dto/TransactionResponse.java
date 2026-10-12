package com.saurabh.financemanager.transaction.dto;

import com.saurabh.financemanager.transaction.Transaction;
import com.saurabh.financemanager.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TransactionResponse {

    @Schema(description = "ID of the transaction", example = "1560751e-33a0-4f25-b9c1-b8916e85c08e",
            accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "Amount in euros", example = "12.50")
    private BigDecimal amount;

    @Schema(description = "What the transaction was for", example = "REWE Frankfurt Hbf")
    private String description;

    @Schema(description = "Date of the transaction", example = "2026-10-05")
    private LocalDate transactionDate;

    @Schema(description = "ID of the transaction's category", example = "55d802f4-e571-4819-b89a-1f4edecedb9c")
    private UUID categoryId;

    @Schema(description = "Name of the transaction's category", example = "Groceries")
    private String categoryName;

    @Schema(description = "Income or expense, taken from the category", example = "EXPENSE")
    private TransactionType transactionType;

    @Schema(description = "True when the category was chosen automatically from keywords", example = "true")
    private boolean autoCategorized;

    @Schema(description = "When the transaction was created", type = "string", example = "2026-10-05T18:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "When the transaction was last changed", type = "string", example = "2026-10-05T18:30:00")
    private LocalDateTime updatedAt;

}
