package com.saurabh.financemanager.category;

import com.saurabh.financemanager.ml.MlClient;
import com.saurabh.financemanager.ml.MlPrediction;
import com.saurabh.financemanager.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final double THRESHOLD = 0.3;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MlClient mlClient;

    private CategorizationService categorizationService;

    private final Category groceries = category("Groceries", TransactionType.EXPENSE, "rewe,lidl,aldi");
    private final Category shopping = category("Shopping", TransactionType.EXPENSE, "amazon,zalando");
    private final Category subscriptions = category("Subscriptions", TransactionType.EXPENSE, "netflix,amazon prime");
    private final Category uncategorized = category("Uncategorized", TransactionType.EXPENSE, null);
    private final Category salary = category("Salary", TransactionType.INCOME, "gehalt,lohn");

    @BeforeEach
    void setUp() {
        categorizationService = new CategorizationService(categoryRepository, mlClient, THRESHOLD);
    }

    // Keyword rules

    @Test
    void shouldReturnRightMatchingCategory() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);

        Optional<Category> result = categorizationService.categorize(USER_ID, "rewe markt frankfurt", TransactionType.EXPENSE);

        assertTrue(result.isPresent());
        assertEquals("Groceries", result.get().getName());
    }

    @Test
    void shouldIgnoreUpperAndLowerCase() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);

        Optional<Category> result = categorizationService.categorize(USER_ID, "EINKAUF BEI LiDl", TransactionType.EXPENSE);

        assertEquals("Groceries", result.get().getName());
    }

    @Test
    void shouldPickLongestKeyword() {
        givenCategories(TransactionType.EXPENSE, shopping, subscriptions);

        Optional<Category> result = categorizationService.categorize(USER_ID, "Amazon Prime Mitgliedschaft", TransactionType.EXPENSE);

        assertEquals("Subscriptions", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenNothingMatches() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);

        Optional<Category> result = categorizationService.categorize(USER_ID, "Birthday present", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    @Test
    void expenseShouldNeverLandInIncomeCategory() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);
        givenCategories(TransactionType.INCOME, salary);

        Optional<Category> asExpense = categorizationService.categorize(USER_ID, "Gehalt Rückzahlung", TransactionType.EXPENSE);
        Optional<Category> asIncome = categorizationService.categorize(USER_ID, "Gehalt Rückzahlung", TransactionType.INCOME);

        assertTrue(asExpense.isEmpty());
        assertEquals("Salary", asIncome.get().getName());
    }

    @Test
    void shouldRejectShortKeywords() {
        assertThrows(IllegalArgumentException.class, () -> KeywordCleaner.clean("rewe, db, lidl"));
        assertEquals("rewe,lidl,aldi", KeywordCleaner.clean(" REWE, lidl,,Aldi, rewe "));
    }

    @Test
    void categoryWithoutKeywordsShouldNeverMatch() {
        givenCategories(TransactionType.EXPENSE, uncategorized);

        Optional<Category> result = categorizationService.categorize(USER_ID, "Uncategorized purchase", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    // ML model

    @Test
    void keywordMatchWinsAndModelIsNotAsked() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);

        Optional<Category> result = categorizationService.categorize(USER_ID, "rewe markt", TransactionType.EXPENSE);

        assertEquals("Groceries", result.get().getName());
        verify(mlClient, never()).predictCategory(any(), any());
    }

    @Test
    void usesModelWhenConfidentAndNoKeywordMatches() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);
        when(mlClient.predictCategory("EDEKA CENTER", TransactionType.EXPENSE))
                .thenReturn(Optional.of(new MlPrediction("Groceries", 0.82)));
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndTransactionType(USER_ID, "Groceries", TransactionType.EXPENSE))
                .thenReturn(Optional.of(groceries));

        Optional<Category> result = categorizationService.categorize(USER_ID, "EDEKA CENTER", TransactionType.EXPENSE);

        assertEquals("Groceries", result.get().getName());
    }

    @Test
    void ignoresModelBelowThreshold() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);
        when(mlClient.predictCategory("PAYPAL BLORX", TransactionType.EXPENSE))
                .thenReturn(Optional.of(new MlPrediction("Shopping", 0.18)));

        Optional<Category> result = categorizationService.categorize(USER_ID, "PAYPAL BLORX", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    @Test
    void ignoresModelWhenUserHasNoSuchCategory() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);
        when(mlClient.predictCategory("SHELL TANKSTELLE", TransactionType.EXPENSE))
                .thenReturn(Optional.of(new MlPrediction("Transport", 0.91)));
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndTransactionType(USER_ID, "Transport", TransactionType.EXPENSE))
                .thenReturn(Optional.empty());

        Optional<Category> result = categorizationService.categorize(USER_ID, "SHELL TANKSTELLE", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    @Test
    void worksWhenModelIsUnavailable() {
        givenCategories(TransactionType.EXPENSE, groceries, shopping);
        when(mlClient.predictCategory("EDEKA CENTER", TransactionType.EXPENSE))
                .thenReturn(Optional.empty());

        Optional<Category> result = categorizationService.categorize(USER_ID, "EDEKA CENTER", TransactionType.EXPENSE);

        assertTrue(result.isEmpty());
    }

    // helpers

    private void givenCategories(TransactionType type, Category... categories) {
        when(categoryRepository.findByUserIdAndTransactionType(USER_ID, type)).thenReturn(List.of(categories));
    }

    private static Category category(String name, TransactionType type, String keywords) {
        Category category = new Category();
        category.setName(name);
        category.setTransactionType(type);
        category.setKeywords(keywords);
        return category;
    }
}