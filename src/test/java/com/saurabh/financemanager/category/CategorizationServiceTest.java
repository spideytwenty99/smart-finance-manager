package com.saurabh.financemanager.category;

import com.saurabh.financemanager.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategorizationService categorizationService;

    private final Category groceries = category("Groceries", TransactionType.EXPENSE, "rewe,lidl,aldi");
    private final Category shopping = category("Shopping", TransactionType.EXPENSE, "amazon,zalando");
    private final Category subscriptions = category("Subscriptions", TransactionType.EXPENSE, "netflix,amazon prime");
    private final Category uncategorized = category("Uncategorized", TransactionType.EXPENSE, null);
    private final Category salary = category("Salary", TransactionType.INCOME, "gehalt,lohn");

    // 1. A matching description returns the right category
    @Test
    void shouldReturnRightMatchingCategory() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(groceries, shopping));

        Optional<Category> result = categorizationService.categorize("rewe markt frankfurt", TransactionType.EXPENSE);

        assertTrue(result.isPresent());
        assertEquals("Groceries", result.get().getName());
    }

    // 2. Matching ignores upper and lower case
    @Test
    void shouldIgnoreUpperAndLowerCase() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(groceries, shopping));

        Optional<Category> result = categorizationService.categorize("EINKAUF BEI LiDl", TransactionType.EXPENSE);

        assertEquals("Groceries", result.get().getName());
    }

    // 3. The longest keyword wins
    @Test
    void shouldPickLongestKeyword() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(shopping, subscriptions));

        Optional<Category> result = categorizationService.categorize("Amazon Prime Mitgliedschaft", TransactionType.EXPENSE);

        assertEquals("Subscriptions", result.get().getName());
    }

    // 4. No match returns nothing
    @Test
    void shouldReturnEmptyWhenNothingMatches() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(groceries, shopping));

        Optional<Category> result = categorizationService.categorize("Birthday present", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    // 5. An expense never lands in an income category
    @Test
    void expenseShouldNeverLandInIncomeCategory() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(groceries, shopping));
        when(categoryRepository.findByTransactionType(TransactionType.INCOME))
                .thenReturn(List.of(salary));

        // "gehalt" is a Salary keyword, but this is an expense
        Optional<Category> asExpense = categorizationService.categorize("Gehalt Rückzahlung", TransactionType.EXPENSE);
        // the same description as income does find Salary
        Optional<Category> asIncome = categorizationService.categorize("Gehalt Rückzahlung", TransactionType.INCOME);

        assertTrue(asExpense.isEmpty());
        assertEquals("Salary", asIncome.get().getName());
    }

    // 6. The short-keyword rule holds
    @Test
    void shouldRejectShortKeywords() {
        assertThrows(IllegalArgumentException.class, () -> KeywordCleaner.clean("rewe, db, lidl"));
        assertEquals("rewe,lidl,aldi", KeywordCleaner.clean(" REWE, lidl,,Aldi, rewe "));
    }

    // 7. A category without keywords never matches
    @Test
    void categoryWithoutKeywordsShouldNeverMatch() {
        when(categoryRepository.findByTransactionType(TransactionType.EXPENSE))
                .thenReturn(List.of(uncategorized));

        Optional<Category> result = categorizationService.categorize("Uncategorized purchase", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    private static Category category(String name, TransactionType type, String keywords) {
        Category category = new Category();
        category.setName(name);
        category.setTransactionType(type);
        category.setKeywords(keywords);
        return category;
    }
}