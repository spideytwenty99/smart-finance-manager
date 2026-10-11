package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.category.BuiltInCategoryInitializer;
import com.saurabh.financemanager.category.CategorizationService;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionRequest;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private record CategoryChoice(Category category,boolean autoCategorized){}

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository,CategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService=categorizationService;
    }

    //Create Transaction
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        CategoryChoice choice = chooseCategory(request);

        Transaction transaction = mapToEntity(request, choice.category());
        transaction.setAutoCategorized(choice.autoCategorized());

        return mapToDto(transactionRepository.save(transaction));

    }

    //Update Transaction
    @Transactional
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction  with id " + id + " not found"));

        CategoryChoice choice = chooseCategory(request);

        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setCategory(choice.category());
        transaction.setAutoCategorized(choice.autoCategorized());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToDto(savedTransaction);

    }

    //Get one transaction by
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction  with id " + id + " not found"));

        return mapToDto(transaction);

    }

    //Get Transaction by dates and categoryID
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate,
                                                     YearMonth month, UUID categoryId) {
        // work out the date range (or none)
        if (month != null && (startDate != null || endDate != null)) {
            throw new IllegalArgumentException("Use either month or startDate/endDate, not both");
        }
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("startDate and endDate must be given together");
        }
        if (month != null) {
            startDate = month.atDay(1);
            endDate = month.atEndOfMonth();
        }
        if (startDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        //picking the query
        boolean hasRange = startDate != null;
        List<Transaction> transactions;

        if (hasRange && categoryId != null) {
            transactions = transactionRepository.findByCategoryIdAndTransactionDateBetween(categoryId, startDate, endDate);
        } else if (hasRange) {
            transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate);
        } else if (categoryId != null) {
            transactions = transactionRepository.findByCategoryId(categoryId);
        } else {
            transactions = transactionRepository.findAll();
        }

        return transactions.stream().map(this::mapToDto).toList();
    }


    //Delete
    @Transactional
    public void deleteTransaction(UUID id){
        Transaction transaction=transactionRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Transaction  with id " + id + " not found"));
        transactionRepository.delete(transaction);
    }

    //Maps to the Response
    private TransactionResponse mapToDto(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();

        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());

        response.setCategoryId(transaction.getCategory().getId());
        response.setCategoryName(transaction.getCategory().getName());
        response.setTransactionType(transaction.getCategory().getTransactionType());
        response.setAutoCategorized(transaction.isAutoCategorized());


        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());

        return response;


    }

    //Decision-making for category
    private CategoryChoice chooseCategory(TransactionRequest request) {
        UUID categoryId = request.getCategoryId();
        TransactionType type = request.getTransactionType();

        // 1. The user picked a category: always respect it
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category with id " + categoryId + " not found"));
            if (type != null && type != category.getTransactionType()) {
                throw new IllegalArgumentException("Category '" + category.getName() + "' is "
                        + category.getTransactionType() + ", but the transaction type is " + type);
            }
            return new CategoryChoice(category, false);
        }

        // 2. No category and no type: we can't tell income from expense
        if (type == null) {
            throw new IllegalArgumentException("Either categoryId or transactionType is required");
        }

        // 3. Let the rules try
        Optional<Category> match = categorizationService.categorize(request.getDescription(), type);
        if (match.isPresent()) {
            return new CategoryChoice(match.get(), true);
        }

        // 4. Nothing matched: Uncategorized for this type
        String code = (type == TransactionType.EXPENSE)
                ? BuiltInCategoryInitializer.UNCATEGORIZED_EXPENSE
                : BuiltInCategoryInitializer.UNCATEGORIZED_INCOME;
        Category fallback = categoryRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Built-in category " + code + " is missing"));
        return new CategoryChoice(fallback, false);
    }


    //Maps to the entity
    private Transaction mapToEntity(TransactionRequest request, Category category) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        transaction.setCategory(category);

        return transaction;

    }


}
