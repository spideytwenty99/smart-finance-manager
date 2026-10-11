package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.category.CategorizationService;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionRequest;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategorizationService categorizationService;

    @InjectMocks
    private TransactionService transactionService;

    private final Category food = category("FOOD", TransactionType.EXPENSE);
    private final Category groceries = category("Groceries", TransactionType.EXPENSE);
    private final Category salary = category("Salary", TransactionType.INCOME);

    @Test
    void shouldCreateTransactionWithChosenCategory() {
        UUID foodId = UUID.randomUUID();
        when(categoryRepository.findById(foodId)).thenReturn(Optional.of(food));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(request(foodId, null, "Lunch"));

        assertEquals("FOOD", response.getCategoryName());
        assertEquals(TransactionType.EXPENSE, response.getTransactionType());
        assertFalse(response.isAutoCategorized());
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(request(unknownId, null, "Lunch")));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldRejectTypeThatDoesNotMatchCategory() {
        UUID salaryId = UUID.randomUUID();
        when(categoryRepository.findById(salaryId)).thenReturn(Optional.of(salary));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(request(salaryId, TransactionType.EXPENSE, "Test")));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldAskRulesWhenNoCategoryIsGiven() {
        when(categorizationService.categorize("REWE Markt", TransactionType.EXPENSE))
                .thenReturn(Optional.of(groceries));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(
                request(null, TransactionType.EXPENSE, "REWE Markt"));

        verify(categorizationService).categorize("REWE Markt", TransactionType.EXPENSE);
        assertEquals("Groceries", response.getCategoryName());
        assertTrue(response.isAutoCategorized());
    }

    @Test
    void shouldNeverOverrideChosenCategory() {
        // "REWE" would match Groceries, but the user picked FOOD
        UUID foodId = UUID.randomUUID();
        when(categoryRepository.findById(foodId)).thenReturn(Optional.of(food));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(request(foodId, null, "REWE Markt"));

        verify(categorizationService, never()).categorize(any(), any());
        assertEquals("FOOD", response.getCategoryName());
        assertFalse(response.isAutoCategorized());
    }

    @Test
    void shouldAskRepositoryForFirstAndLastDayOfMonth() {
        ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);

        // February 2026: 28 days, a good test for "last day"
        transactionService.getTransactions(null, null, YearMonth.of(2026, 2), null);

        verify(transactionRepository).findByTransactionDateBetween(start.capture(), end.capture());
        assertEquals(LocalDate.of(2026, 2, 1), start.getValue());
        assertEquals(LocalDate.of(2026, 2, 28), end.getValue());
    }


    private void givenSaveReturnsWhatItGets() {
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private static TransactionRequest request(UUID categoryId, TransactionType type, String description) {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("12.50"));
        request.setDescription(description);
        request.setTransactionDate(LocalDate.of(2026, 10, 11));
        request.setCategoryId(categoryId);
        request.setTransactionType(type);
        return request;
    }

    private static Category category(String name, TransactionType type) {
        Category category = new Category();
        category.setName(name);
        category.setTransactionType(type);
        return category;
    }
}