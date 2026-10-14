package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.authentication.AuthService;
import com.saurabh.financemanager.category.CategorizationService;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.category.DefaultCategories;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionRequest;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import com.saurabh.financemanager.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {


    private record CategoryChoice(Category category, boolean autoCategorized) {}

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              CategorizationService categorizationService,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
        this.userRepository = userRepository;
    }

    // Create
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionRequest request) {
        CategoryChoice choice = chooseCategory(userId, request);

        Transaction transaction = mapToEntity(request, choice.category());
        transaction.setUser(userRepository.getReferenceById(userId));
        transaction.setAutoCategorized(choice.autoCategorized());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction {} created for user {}", saved.getId(), userId);
        if (choice.autoCategorized()) {
            log.info("Transaction {} categorized automatically into category {}", saved.getId(), choice.category().getId());
        }
        return mapToDto(saved);
    }

    // Update
    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID id, TransactionRequest request) {
        Transaction transaction = findOwned(userId, id);
        CategoryChoice choice = chooseCategory(userId, request);

        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setCategory(choice.category());
        transaction.setAutoCategorized(choice.autoCategorized());

        return mapToDto(transactionRepository.save(transaction));
    }

    // Get one
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, UUID id) {
        return mapToDto(findOwned(userId, id));
    }

    // List, filtered by date range and/or category, always for this user only
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID userId, LocalDate startDate, LocalDate endDate,
                                                     YearMonth month, UUID categoryId) {
        // Work out the date range (or none)
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

        // Pick the query
        boolean hasRange = startDate != null;
        List<Transaction> transactions;

        if (hasRange && categoryId != null) {
            transactions = transactionRepository.findByUserIdAndCategoryIdAndTransactionDateBetween(
                    userId, categoryId, startDate, endDate);
        } else if (hasRange && categoryId == null) {
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        } else if (!hasRange && categoryId != null) {
            transactions = transactionRepository.findByUserIdAndCategoryId(userId, categoryId);
        } else {
            transactions = transactionRepository.findByUserId(userId);
        }

        return transactions.stream().map(this::mapToDto).toList();
    }

    // Delete
    @Transactional
    public void deleteTransaction(UUID userId, UUID id) {
        transactionRepository.delete(findOwned(userId, id));
    }

    // Decides the category; every lookup is limited to this user's categories
    private CategoryChoice chooseCategory(UUID userId, TransactionRequest request) {
        UUID categoryId = request.getCategoryId();
        TransactionType type = request.getTransactionType();

        // 1. The user picked a category: it must be one of their own
        if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category with id " + categoryId + " not found"));
            if (type != null && type != category.getTransactionType()) {
                throw new IllegalArgumentException("Category '" + category.getName() + "' is "
                        + category.getTransactionType() + ", but the transaction type is " + type);
            }
            return new CategoryChoice(category, false);
        }

        // 2. No category and no type: can't tell income from expense
        if (type == null) {
            throw new IllegalArgumentException("Either categoryId or transactionType is required");
        }

        // 3. Let the rules try, using only this user's categories
        Optional<Category> match = categorizationService.categorize(userId, request.getDescription(), type);
        if (match.isPresent()) {
            return new CategoryChoice(match.get(), true);
        }

        // 4. Nothing matched: this user's Uncategorized for the type
        String code = (type == TransactionType.EXPENSE)
                ? DefaultCategories.UNCATEGORIZED_EXPENSE
                : DefaultCategories.UNCATEGORIZED_INCOME;
        Category fallback = categoryRepository.findByUserIdAndCode(userId, code)
                .orElseThrow(() -> new IllegalStateException("Category " + code + " is missing for this user"));
        return new CategoryChoice(fallback, false);
    }

    // The one place that enforces ownership for transactions
    private Transaction findOwned(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction with id " + id + " not found"));
    }

    private Transaction mapToEntity(TransactionRequest request, Category category) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setCategory(category);
        return transaction;
    }

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
}