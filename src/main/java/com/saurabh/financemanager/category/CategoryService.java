package com.saurabh.financemanager.category;

import com.saurabh.financemanager.budget.BudgetRepository;
import com.saurabh.financemanager.budget.BudgetService;
import com.saurabh.financemanager.category.dto.CategoryRequest;
import com.saurabh.financemanager.category.dto.CategoryResponse;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.exception.ConflictException;
import com.saurabh.financemanager.transaction.TransactionRepository;
import com.saurabh.financemanager.transaction.TransactionType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }


    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        String name = categoryRequest.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndTransactionType(name, categoryRequest.getTransactionType())) {
            throw new DuplicateResourceException(
                    "Category '" + name + "' already exists for type " + categoryRequest.getTransactionType()
            );
        }

        Category category = mapToEntity(categoryRequest);
        category.setName(name);

        category.setKeywords(KeywordCleaner.clean(categoryRequest.getKeywords()));

        Category savedCategory = categoryRepository.save(category);

        return mapToDto(savedCategory);

    }

    public CategoryResponse getCategory(UUID id) {
        Category response = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category Not found"));
        return mapToDto(response);
    }

    public List<CategoryResponse> getAllCategory(TransactionType type) {
        List<Category> categoryList = (type == null) ?
                categoryRepository.findAll()
                : categoryRepository.findByTransactionType(type);

        return categoryList.stream()
                .map(this::mapToDto).toList();
    }

    public void delete(UUID id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));

        if (category.isSystemCategory()) {
            throw new ConflictException("Built-in category '" + category.getName() + "' can't be deleted");
        }

        long categoryUsage = transactionRepository.countByCategoryId(id);

        long budgetUsage = budgetRepository.countByCategoryId(id);

        if (categoryUsage > 0) {
            throw new ConflictException(
                    "Category '" + category.getName() + "' is used by " + categoryUsage + " transactions and cannot be deleted"
            );
        }
        if (budgetUsage > 0) {
            throw new ConflictException(
                    "Category '" + category.getName() + "' is used by " + budgetUsage + " budgets and cannot be deleted"
            );
        }


        categoryRepository.delete(category);
    }


    private Category mapToEntity(CategoryRequest categoryRequest) {
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setTransactionType(categoryRequest.getTransactionType());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        return category;
    }

    private CategoryResponse mapToDto(Category category) {
        CategoryResponse responseDto = new CategoryResponse();

        responseDto.setId(category.getId());
        responseDto.setName(category.getName());
        responseDto.setCreatedAt(category.getCreatedAt());
        responseDto.setUpdatedAt(category.getUpdatedAt());
        responseDto.setTransactionType(category.getTransactionType());
        responseDto.setKeywords(category.getKeywords());
        responseDto.setSystemCategory(category.isSystemCategory());

        return responseDto;
    }


    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));

        String name = request.getName().trim();
        boolean renamed = !category.getName().equalsIgnoreCase(name);
        boolean transactionTypeChanged = category.getTransactionType() != request.getTransactionType();

        if (category.isSystemCategory() && (renamed || transactionTypeChanged)) {
            throw new ConflictException("Built-in category '" + category.getName()
                    + "' can't be renamed or change its type, only its keywords can be edited");
        }

        if ((renamed || transactionTypeChanged)
                && categoryRepository.existsByNameIgnoreCaseAndTransactionType(name, request.getTransactionType())) {
            throw new DuplicateResourceException(
                    "Category '" + name + "' already exists for type " + request.getTransactionType());
        }

        category.setName(name);
        category.setTransactionType(request.getTransactionType());
        category.setKeywords(KeywordCleaner.clean(request.getKeywords()));

        return mapToDto(categoryRepository.save(category));
    }
}
