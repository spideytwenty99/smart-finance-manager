package com.saurabh.financemanager.report;

import com.saurabh.financemanager.budget.Budget;
import com.saurabh.financemanager.budget.BudgetRepository;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.report.dto.MonthlySummary;
import com.saurabh.financemanager.report.dto.SpentPerCategory;
import com.saurabh.financemanager.transaction.TransactionRepository;
import com.saurabh.financemanager.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private ReportService reportService;

    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    // The categories
    private final Category food = category("Food");
    private final Category transport = category("Transport");
    private final Category rent = category("Rent");
    private final Category uncategorized = category("Uncategorized");
    private final Category gym = category("Gym");


    @Test
    void shouldCalculateTotalsAndBalance() {
        givenSampleMonth();

        MonthlySummary summary = reportService.getMonthlySummary(AUGUST);

        assertAmount("2000", summary.totalIncome());
        assertAmount("1060", summary.totalExpenses());
        assertAmount("940", summary.balance());
    }


    @Test
    void shouldBeOkJustBelow80Percent() {
        SpentPerCategory row = singleRow("79.99", "100");

        assertEquals(BudgetStatus.OK, row.budgetStatus());
    }

    @Test
    void shouldBeWarningAtExactly80Percent() {
        SpentPerCategory row = singleRow("80", "100");

        assertEquals(BudgetStatus.WARNING, row.budgetStatus());
        assertAmount("80", row.budgetUsedInPercentage());
    }

    @Test
    void shouldNotBeOverAtExactly100Percent() {
        SpentPerCategory row = singleRow("100", "100");

        assertEquals(BudgetStatus.WARNING, row.budgetStatus());
        assertAmount("0", row.balance());
    }

    @Test
    void shouldBeOverJustAbove100Percent() {
        SpentPerCategory row = singleRow("100.01", "100");

        assertEquals(BudgetStatus.OVER_LIMIT, row.budgetStatus());
        assertAmount("-0.01", row.balance());
    }


    @Test
    void shouldCalculateBudgetLinesLikeStep6() {
        givenSampleMonth();

        List<SpentPerCategory> rows = reportService.getMonthlySummary(AUGUST).spentPerCategoriesList();

        SpentPerCategory foodRow = row(rows, "Food");
        assertAmount("50", foodRow.balance());
        assertAmount("83.33", foodRow.budgetUsedInPercentage());
        assertEquals(BudgetStatus.WARNING, foodRow.budgetStatus());

        SpentPerCategory transportRow = row(rows, "Transport");
        assertAmount("-10", transportRow.balance());
        assertEquals(BudgetStatus.OVER_LIMIT, transportRow.budgetStatus());
    }

    @Test
    void shouldShowBudgetWithNoSpending() {
        givenSampleMonth();

        List<SpentPerCategory> rows = reportService.getMonthlySummary(AUGUST).spentPerCategoriesList();

        SpentPerCategory gymRow = row(rows, "Gym");
        assertAmount("0", gymRow.totalSpent());
        assertAmount("30", gymRow.balance());
        assertEquals(BudgetStatus.OK, gymRow.budgetStatus());
    }

    @Test
    void shouldGiveUncategorizedExpensesTheirOwnLine() {
        givenSampleMonth();

        List<SpentPerCategory> rows = reportService.getMonthlySummary(AUGUST).spentPerCategoriesList();

        SpentPerCategory uncategorizedRow = row(rows, "Uncategorized");
        assertAmount("20", uncategorizedRow.totalSpent());
        assertNull(uncategorizedRow.budgetLimit());
        assertEquals(BudgetStatus.NOT_SET, uncategorizedRow.budgetStatus());
    }

    @Test
    void shouldSortLinesBySpendingHighestFirst() {
        givenSampleMonth();

        List<String> names = reportService.getMonthlySummary(AUGUST).spentPerCategoriesList()
                .stream()
                .map(SpentPerCategory::categoryName)
                .toList();

        assertEquals(List.of("Rent", "Food", "Transport", "Uncategorized", "Gym"), names);
    }

    // ---------- empty and null ----------

    @Test
    void shouldGiveZerosForEmptyMonth() {
        givenMonth(BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());

        MonthlySummary summary = reportService.getMonthlySummary(AUGUST);

        assertAmount("0", summary.totalIncome());
        assertAmount("0", summary.totalExpenses());
        assertAmount("0", summary.balance());
        assertTrue(summary.spentPerCategoriesList().isEmpty());
    }

    @Test
    void shouldNotCrashWhenSumsAreNull() {
        givenMonth(null, null, List.of(), List.of());

        MonthlySummary summary = reportService.getMonthlySummary(AUGUST);

        assertAmount("0", summary.totalIncome());
        assertAmount("0", summary.totalExpenses());
        assertAmount("0", summary.balance());
    }

    private void givenSampleMonth() {
        givenMonth(
                new BigDecimal("2000"),
                new BigDecimal("1060"),
                List.of(
                        total(rent, "700"),
                        total(food, "250"),
                        total(transport, "90"),
                        total(uncategorized, "20")),
                List.of(
                        budget(food, "300"),
                        budget(transport, "80"),
                        budget(gym, "30")));
    }

    private void givenMonth(BigDecimal income, BigDecimal expenses,
                            List<TotalByCategory> totals, List<Budget> budgets) {
        when(transactionRepository.sumByType(any(), any(), eq(TransactionType.INCOME))).thenReturn(income);
        when(transactionRepository.sumByType(any(), any(), eq(TransactionType.EXPENSE))).thenReturn(expenses);
        when(transactionRepository.totalPerCategory(any(), any(), eq(TransactionType.EXPENSE))).thenReturn(totals);
        when(budgetRepository.findByMonth(AUGUST.atDay(1))).thenReturn(budgets);
    }

    // One category, one budget: returns its line
    private SpentPerCategory singleRow(String spent, String limit) {
        givenMonth(BigDecimal.ZERO, new BigDecimal(spent),
                List.of(total(food, spent)),
                List.of(budget(food, limit)));
        return reportService.getMonthlySummary(AUGUST).spentPerCategoriesList().get(0);
    }

    private static Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setTransactionType(TransactionType.EXPENSE);
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    private static TotalByCategory total(Category category, String amount) {
        return new TotalByCategory(category.getId(), category.getName(), new BigDecimal(amount));
    }

    private static Budget budget(Category category, String limit) {
        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setMonth(AUGUST.atDay(1));
        budget.setAmountLimit(new BigDecimal(limit));
        return budget;
    }

    private static SpentPerCategory row(List<SpentPerCategory> rows, String name) {
        return rows.stream()
                .filter(r -> r.categoryName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line for " + name));
    }

    // Compares by value, so 50 and 50.00 count as equal
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }
}