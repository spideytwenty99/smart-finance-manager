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
import com.saurabh.financemanager.user.User;
import com.saurabh.financemanager.user.UserRepository;
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
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                           BudgetRepository budgetRepository,UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository=userRepository;
    }


    @Transactional
    public CategoryResponse createCategory(UUID userId, CategoryRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndTransactionType(
                userId, name, request.getTransactionType())) {
            throw new DuplicateResourceException(
                    "Category '" + name + "' already exists for type " + request.getTransactionType());
        }

        Category category = new Category();
        category.setUser(userRepository.getReferenceById(userId));
        category.setName(name);
        category.setTransactionType(request.getTransactionType());
        category.setKeywords(KeywordCleaner.clean(request.getKeywords()));

        return mapToDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID userId, UUID id) {
        return mapToDto(findOwned(userId, id));
    }


    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(UUID userId, TransactionType type) {
        List<Category> categories = (type == null)
                ? categoryRepository.findByUserId(userId)
                : categoryRepository.findByUserIdAndTransactionType(userId, type);
        return categories.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Category category = findOwned(userId, id);

        if (category.isSystemCategory()) {
            throw new ConflictException("Category '" + category.getName() + "' can't be deleted");
        }

        long transactionCount = transactionRepository.countByCategoryId(id);
        if (transactionCount > 0) {
            throw new ConflictException("Category '" + category.getName() + "' is used by "
                    + transactionCount + " transactions and cannot be deleted");
        }

        long budgetCount = budgetRepository.countByCategoryId(id);
        if (budgetCount > 0) {
            throw new ConflictException("Category '" + category.getName() + "' is used by "
                    + budgetCount + " budgets and cannot be deleted");
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
    public CategoryResponse update(UUID userId, UUID id, CategoryRequest request) {
        Category category = findOwned(userId, id);

        String name = request.getName().trim();
        boolean renamed = !category.getName().equalsIgnoreCase(name);
        boolean retyped = category.getTransactionType() != request.getTransactionType();

        if (category.isSystemCategory() && (renamed || retyped)) {
            throw new ConflictException("Category '" + category.getName()
                    + "' can't be renamed or change its type, only its keywords can be edited");
        }

        if ((renamed || retyped) && categoryRepository.existsByUserIdAndNameIgnoreCaseAndTransactionType(
                userId, name, request.getTransactionType())) {
            throw new DuplicateResourceException(
                    "Category '" + name + "' already exists for type " + request.getTransactionType());
        }

        category.setName(name);
        category.setTransactionType(request.getTransactionType());
        category.setKeywords(KeywordCleaner.clean(request.getKeywords()));

        return mapToDto(categoryRepository.save(category));
    }


    @Transactional
    public void createDefaultsFor(User user) {
        List<Category> defaults = DefaultCategories.ALL.stream()
                .map(d -> {
                    Category category = new Category();
                    category.setUser(user);
                    category.setName(d.name());
                    category.setTransactionType(d.type());
                    category.setKeywords(KeywordCleaner.clean(d.keywords()));
                    category.setCode(d.code());
                    category.setSystemCategory(d.code() != null);
                    return category;
                })
                .toList();

        categoryRepository.saveAll(defaults);
    }
    // The one place that enforces ownership for categories
    private Category findOwned(UUID userId, UUID id) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }
}
