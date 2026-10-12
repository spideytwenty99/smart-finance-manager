package com.saurabh.financemanager.category.dto;


import com.saurabh.financemanager.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter

public class CategoryResponse {

    @Schema(description = "ID of the category", example = "55d802f4-e571-4819-b89a-1f4edecedb9c",
            accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "Name of the category", example = "Groceries")
    private String name;

    @Schema(description = "Income or expense", example = "EXPENSE")
    private TransactionType transactionType;

    @Schema(description = "Cleaned keywords used for automatic categorization", example = "rewe,lidl,aldi")
    private String keywords;

    @Schema(description = "True for built-in categories, which can't be renamed or deleted", example = "true")
    private boolean systemCategory;

    @Schema(description = "When the category was created", type = "string", example = "2026-10-05T20:42:45")
    private LocalDateTime createdAt;

    @Schema(description = "When the category was last changed", type = "string", example = "2026-10-11T13:18:08")
    private LocalDateTime updatedAt;
}
