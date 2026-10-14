package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.category.CategorizationService;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.category.DefaultCategories;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionRequest;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import com.saurabh.financemanager.user.User;
import com.saurabh.financemanager.user.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategorizationService categorizationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final Category food = category("FOOD", TransactionType.EXPENSE);
    private final Category groceries = category("Groceries", TransactionType.EXPENSE);
    private final Category salary = category("Salary", TransactionType.INCOME);
    private final Category uncategorized = category("Uncategorized", TransactionType.EXPENSE);

    // choosing the category

    @Test
    void shouldCreateTransactionWithChosenCategory() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(food));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(USER_ID, request(CATEGORY_ID, null, "Lunch"));

        assertEquals("FOOD", response.getCategoryName());
        assertEquals(TransactionType.EXPENSE, response.getTransactionType());
        assertFalse(response.isAutoCategorized());
    }

    @Test
    void shouldRejectCategoryThatIsMissingOrBelongsToAnotherUser() {
        // To this user, another user's category looks exactly like a missing one
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(USER_ID, request(CATEGORY_ID, null, "Lunch")));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldRejectTypeThatDoesNotMatchCategory() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(salary));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(USER_ID, request(CATEGORY_ID, TransactionType.EXPENSE, "Test")));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldAskRulesWhenNoCategoryIsGiven() {
        when(categorizationService.categorize(USER_ID, "REWE Markt", TransactionType.EXPENSE))
                .thenReturn(Optional.of(groceries));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(
                USER_ID, request(null, TransactionType.EXPENSE, "REWE Markt"));

        verify(categorizationService).categorize(USER_ID, "REWE Markt", TransactionType.EXPENSE);
        assertEquals("Groceries", response.getCategoryName());
        assertTrue(response.isAutoCategorized());
    }

    @Test
    void shouldFallBackToUsersOwnUncategorizedWhenNothingMatches() {
        when(categorizationService.categorize(USER_ID, "Birthday present", TransactionType.EXPENSE))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByUserIdAndCode(USER_ID, DefaultCategories.UNCATEGORIZED_EXPENSE))
                .thenReturn(Optional.of(uncategorized));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(
                USER_ID, request(null, TransactionType.EXPENSE, "Birthday present"));

        assertEquals("Uncategorized", response.getCategoryName());
        assertFalse(response.isAutoCategorized());
    }

    @Test
    void shouldNeverOverrideChosenCategory() {
        // "REWE" would match Groceries, but the user picked FOOD
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(food));
        givenSaveReturnsWhatItGets();

        TransactionResponse response = transactionService.createTransaction(
                USER_ID, request(CATEGORY_ID, null, "REWE Markt"));

        verify(categorizationService, never()).categorize(any(), any(), any());
        assertEquals("FOOD", response.getCategoryName());
        assertFalse(response.isAutoCategorized());
    }

    // ownership

    @Test
    void shouldSetCurrentUserAsOwner() {
        User currentUser = new User();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(currentUser);
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(food));
        givenSaveReturnsWhatItGets();

        transactionService.createTransaction(USER_ID, request(CATEGORY_ID, null, "Lunch"));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertSame(currentUser, saved.getValue().getUser());
    }

    @Test
    void shouldNotReturnTransactionOfAnotherUser() {
        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransaction(USER_ID, TRANSACTION_ID));
    }

    // filters

    @Test
    void shouldAskRepositoryForFirstAndLastDayOfMonth() {
        ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);

        // February 2026: 28 days, a good test for "last day"
        transactionService.getTransactions(USER_ID, null, null, YearMonth.of(2026, 2), null);

        verify(transactionRepository).findByUserIdAndTransactionDateBetween(eq(USER_ID), start.capture(), end.capture());
        assertEquals(LocalDate.of(2026, 2, 1), start.getValue());
        assertEquals(LocalDate.of(2026, 2, 28), end.getValue());
    }

    // helpers

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