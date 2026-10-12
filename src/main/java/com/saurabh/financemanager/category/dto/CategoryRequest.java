package com.saurabh.financemanager.category.dto;


import com.saurabh.financemanager.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    @Schema(description = "Name of the category, unique per type", example = "Groceries")
    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must be at most 50 characters")
    private String name;

    @Schema(description = "Whether this is an income or an expense category", example = "EXPENSE")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Schema(description = "Optional comma-separated keywords for automatic categorization; each needs at least 3 letters",
            example = "rewe,lidl,aldi")
    @Size(max = 500, message = "Keywords must be at most 500 characters")
    private String keywords;

}
