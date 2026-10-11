package com.saurabh.financemanager.category;


import com.saurabh.financemanager.transaction.Transaction;
import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTransactionType(TransactionType transactionType);
    boolean existsByNameIgnoreCaseAndTransactionType(String name,TransactionType transactionType);

    Optional<Category> findByCode(String code);

    Optional<Category> findByNameIgnoreCaseAndTransactionType(String name, TransactionType type);
}
