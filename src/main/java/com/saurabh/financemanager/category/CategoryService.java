package com.saurabh.financemanager.category;

import com.saurabh.financemanager.category.dto.CategoryRequest;
import com.saurabh.financemanager.category.dto.CategoryResponse;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.exception.ConflictException;
import com.saurabh.financemanager.transaction.TransactionRepository;
import com.saurabh.financemanager.transaction.TransactionType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository=transactionRepository;
    }


    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        String name=categoryRequest.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndTransactionType(name, categoryRequest.getTransactionType())){
            throw new DuplicateResourceException(
                    "Category '"+name+"' already exists for type "+categoryRequest.getTransactionType()
            );
        }

        Category category = mapToEntity(categoryRequest);
        category.setName(name);

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
                .orElseThrow(() -> new RuntimeException("Category not found"));

        long categoryUsage= transactionRepository.countByCategoryId(id);

        if (categoryUsage>0){
            throw new ConflictException(
                    "Category '" +category.getName() + "' is used by " + categoryUsage+ " transactions and cannot be delted"
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

        return responseDto;
    }


    public CategoryResponse update(UUID id, CategoryRequest categoryRequest) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        existingCategory.setName(categoryRequest.getName());
        existingCategory.setTransactionType(categoryRequest.getTransactionType());


        Category updatedCategory = categoryRepository.save(existingCategory);

        return mapToDto(updatedCategory);

    }
}
