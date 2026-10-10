package com.saurabh.financemanager.report;

import com.saurabh.financemanager.budget.Budget;
import com.saurabh.financemanager.budget.BudgetRepository;
import com.saurabh.financemanager.report.dto.MonthlySummary;
import com.saurabh.financemanager.report.dto.SpentPerCategory;
import com.saurabh.financemanager.transaction.TransactionRepository;
import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public ReportService(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySummary getMonthlySummary(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumByType(start, end, TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumByType(start, end, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<TotalByCategory> totals = transactionRepository.totalPerCategory(start, end, TransactionType.EXPENSE);
        List<Budget> budgets = budgetRepository.findByMonth(start);

        List<SpentPerCategory> rows = buildAllCategoryRows(totals, budgets);

        return new MonthlySummary(month, totalIncome, totalExpense, balance, rows);
    }

    // Builds the whole table: one row per category, sorted by spending
    private List<SpentPerCategory> buildAllCategoryRows(List<TotalByCategory> totals, List<Budget> budgets) {
        Map<UUID, Budget> budgetByCategory = new HashMap<>();
        for (Budget budget : budgets) {
            budgetByCategory.put(budget.getCategory().getId(), budget);
        }

        List<SpentPerCategory> rows = new ArrayList<>();

        // Categories that had spending this month
        for (TotalByCategory t : totals) {
            Budget budget = budgetByCategory.remove(t.categoryId());
            rows.add(buildOneCategoryRow(t.categoryId(), t.categoryName(), t.totalSpent(), budget));
        }

        // Budgets still left had no spending at all this month
        for (Budget budget : budgetByCategory.values()) {
            rows.add(buildOneCategoryRow(budget.getCategory().getId(), budget.getCategory().getName(),
                    BigDecimal.ZERO, budget));
        }

        rows.sort(Comparator.comparing(SpentPerCategory::totalSpent).reversed());
        return rows;
    }

    // Builds one row: with budget fields if there is a budget, empty otherwise
    private SpentPerCategory buildOneCategoryRow(UUID categoryId, String categoryName,
                                                 BigDecimal spent, Budget budget) {
        if (budget == null) {
            return new SpentPerCategory(categoryId, categoryName, spent,
                    null, null, null, BudgetStatus.NOT_SET);
        }

        BigDecimal limit = budget.getAmountLimit();
        BigDecimal remaining = limit.subtract(spent);
        BigDecimal percentUsed = calculatePercentage(spent, limit);
        BudgetStatus status = getBudgetStatus(spent, limit, percentUsed);

        return new SpentPerCategory(categoryId, categoryName, spent, limit, remaining, percentUsed, status);
    }

    private BudgetStatus getBudgetStatus(BigDecimal spent, BigDecimal limit, BigDecimal percentUsed) {
        if (spent.compareTo(limit) > 0) {
            return BudgetStatus.OVER_LIMIT;
        }
        if (percentUsed.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }

    private BigDecimal calculatePercentage(BigDecimal spent, BigDecimal limit) {
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, RoundingMode.HALF_UP);
    }
}