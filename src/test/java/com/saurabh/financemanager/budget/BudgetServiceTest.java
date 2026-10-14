package com.saurabh.financemanager.budget;

import com.saurabh.financemanager.budget.dto.BudgetRequest;
import com.saurabh.financemanager.budget.dto.BudgetResponse;
import com.saurabh.financemanager.budget.dto.BudgetUpdateRequest;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.TransactionType;
import com.saurabh.financemanager.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void shouldThrowWhenBudgetAlreadyExistsForMonth() {
        Category groceries = category("Groceries", TransactionType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(groceries));
        when(budgetRepository.existsByCategoryIdAndMonth(any(), any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> budgetService.createBudget(USER_ID, request("300.00")));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectBudgetForIncomeCategory() {
        Category salary = category("Salary", TransactionType.INCOME);
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(salary));

        assertThrows(IllegalArgumentException.class,
                () -> budgetService.createBudget(USER_ID, request("1000.00")));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectCategoryOfAnotherUser() {
        // The category exists, but not for this user, so the repository finds nothing
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.createBudget(USER_ID, request("300.00")));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldChangeOnlyLimitOnUpdate() {
        Category groceries = category("Groceries", TransactionType.EXPENSE);
        Budget existing = new Budget();
        existing.setCategory(groceries);
        existing.setMonth(LocalDate.of(2026, 10, 1));
        existing.setAmountLimit(new BigDecimal("300.00"));

        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(call -> call.getArgument(0));

        BudgetUpdateRequest update = new BudgetUpdateRequest();
        update.setAmountLimit(new BigDecimal("350.00"));

        BudgetResponse response = budgetService.updateBudget(USER_ID, BUDGET_ID, update);

        assertEquals(new BigDecimal("350.00"), response.getAmountLimit());
        assertEquals("Groceries", response.getCategoryName());
        assertEquals(YearMonth.of(2026, 10), response.getMonth());
        assertEquals(LocalDate.of(2026, 10, 1), existing.getMonth());
        assertEquals(groceries, existing.getCategory());
    }

    @Test
    void shouldNotUpdateBudgetOfAnotherUser() {
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

        BudgetUpdateRequest update = new BudgetUpdateRequest();
        update.setAmountLimit(new BigDecimal("350.00"));

        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.updateBudget(USER_ID, BUDGET_ID, update));

        verify(budgetRepository, never()).save(any());
    }

    // helpers

    private static Category category(String name, TransactionType type) {
        Category category = new Category();
        category.setName(name);
        category.setTransactionType(type);
        return category;
    }

    private static BudgetRequest request(String limit) {
        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(CATEGORY_ID);
        request.setMonth(YearMonth.of(2026, 10));
        request.setAmountLimit(new BigDecimal(limit));
        return request;
    }
}