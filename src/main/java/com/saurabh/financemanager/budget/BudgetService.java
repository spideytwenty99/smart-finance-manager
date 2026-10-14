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
    private  final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository=userRepository;
    }

    @Transactional
    public BudgetResponse createBudget(UUID userId, BudgetRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
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

        Budget budget = mapToEntity(request, category);
        budget.setUser(userRepository.getReferenceById(userId));

        return mapToDto(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID userId, UUID id) {
        return mapToDto(findOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForMonth(UUID userId, YearMonth month) {
        return budgetRepository.findByUserIdAndMonth(userId, month.atDay(1))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID id, BudgetUpdateRequest request) {
        Budget budget = findOwned(userId, id);
        budget.setAmountLimit(request.getAmountLimit());
        return mapToDto(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID id) {
        budgetRepository.delete(findOwned(userId, id));
    }

    private Budget findOwned(UUID userId, UUID id) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + id + " not found"));
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