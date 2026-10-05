package com.saurabh.financemanager.category.dto;


import com.saurabh.financemanager.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter

public class CategoryResponse {

    private UUID id;

    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private TransactionType transactionType;

}
