package com.saurabh.financemanager.budget;

import com.saurabh.financemanager.budget.dto.BudgetRequest;
import com.saurabh.financemanager.budget.dto.BudgetResponse;
import com.saurabh.financemanager.budget.dto.BudgetUpdateRequest;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    // Create
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + request.getCategoryId() + " not found"));

        if (category.getTransactionType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set for expense categories");
        }

        LocalDate monthStart = request.getMonth().atDay(1);
        if (budgetRepository.existsByCategoryIdAndMonth(category.getId(), monthStart)) {
            throw new DuplicateResourceException(
                    "Category '" + category.getName() + "' already has a budget for " + request.getMonth());
        }

        Budget savedBudget = budgetRepository.save(mapToEntity(request, category));
        return mapToDto(savedBudget);
    }

    // Get one budget by its id
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + id + " not found"));
        return mapToDto(budget);
    }

    // Get all budgets for a month
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForMonth(YearMonth month) {
        return budgetRepository.findByMonth(month.atDay(1))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // Update the limit
    @Transactional
    public BudgetResponse updateBudget(UUID id, BudgetUpdateRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + id + " not found"));

        budget.setAmountLimit(request.getAmountLimit());
        return mapToDto(budgetRepository.save(budget));
    }

    // Delete
    @Transactional
    public void deleteBudget(UUID id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + id + " not found"));
        budgetRepository.delete(budget);
    }

    private Budget mapToEntity(BudgetRequest request, Category category) {
        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setMonth(request.getMonth().atDay(1));
        budget.setAmountLimit(request.getAmountLimit());
        return budget;
    }

    private BudgetResponse mapToDto(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategoryId(budget.getCategory().getId());
        response.setCategoryName(budget.getCategory().getName());
        response.setAmountLimit(budget.getAmountLimit());
        response.setMonth(YearMonth.from(budget.getMonth()));
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        return response;
    }
}