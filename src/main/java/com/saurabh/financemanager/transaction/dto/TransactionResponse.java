package com.saurabh.financemanager.transaction.dto;

import com.saurabh.financemanager.transaction.Transaction;
import com.saurabh.financemanager.transaction.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;


    private UUID categoryID;
    private String categoryName;
    private TransactionType transactionType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
