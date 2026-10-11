package com.saurabh.financemanager.category.dto;


import com.saurabh.financemanager.transaction.TransactionType;
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

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must be at most 50 characters")
    private String name;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    //For category created by user to teach the app it's keywords
    @Size(max = 500, message = "Keywords must be at most 500 characters")
    private String keywords;

}
