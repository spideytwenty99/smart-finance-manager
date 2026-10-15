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


    List<Category> findByUserId(UUID userId);

    List<Category> findByUserIdAndTransactionType(UUID userId, TransactionType transactionType);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndTransactionType(UUID userId, String name, TransactionType transactionType);

    Optional<Category> findByUserIdAndCode(UUID userId, String code);


    Optional<Category> findByUserIdAndNameIgnoreCaseAndTransactionType(UUID userId, String name, TransactionType transactionType);

}
