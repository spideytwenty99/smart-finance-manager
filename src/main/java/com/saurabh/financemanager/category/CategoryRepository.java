package com.saurabh.financemanager.category;


import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTransactionType(TransactionType transactionType);
    boolean existsByNameIgnoreCaseAndTransactionType(String name,TransactionType transactionType);

}
