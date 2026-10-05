package com.saurabh.financemanager.category;


import com.saurabh.financemanager.category.dto.CategoryRequest;
import com.saurabh.financemanager.category.dto.CategoryResponse;
import com.saurabh.financemanager.transaction.TransactionType;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category")

public class CategoryController {
    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService=categoryService;
    }

    //Create category
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest){

        CategoryResponse categoryResponse= categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
    }

    //Get Category by ID
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id){

        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    //Get all categories
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategoires(@RequestParam(required=false)TransactionType type){

        return ResponseEntity.ok(categoryService.getAllCategory(type));    }


    //Update Category
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest categoryRequest){
        CategoryResponse response=categoryService.update(id, categoryRequest);
        return ResponseEntity.ok(response);
    }


    //Delete Category2
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable UUID id){
        categoryService.delete(id);
        return ResponseEntity.noContent().build();

    }
}
