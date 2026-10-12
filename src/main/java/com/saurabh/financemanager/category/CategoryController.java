package com.saurabh.financemanager.category;

import com.saurabh.financemanager.category.dto.CategoryRequest;
import com.saurabh.financemanager.category.dto.CategoryResponse;
import com.saurabh.financemanager.exception.ErrorResponse;
import com.saurabh.financemanager.transaction.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = "Income and expense categories, including built-in ones with keywords for automatic categorization.")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category",
            description = "Names must be unique per type, ignoring upper and lower case. "
                    + "Keywords are optional and used for automatic categorization; they are saved "
                    + "lowercase and trimmed, duplicates are removed, and each keyword needs at least 3 letters.")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Invalid input, or a keyword shorter than 3 letters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A category with this name and type already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryResponse response = categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a category by id")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No category with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @Parameter(description = "ID of the category") @PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @Operation(summary = "List categories",
            description = "Returns all categories, including the built-in ones. Optionally filter by type.")
    @ApiResponse(responseCode = "200", description = "List of categories, possibly empty")
    @ApiResponse(responseCode = "400", description = "Unknown type value",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @Parameter(description = "Optional filter: only categories of this type. Without it, all categories are returned.")
            @RequestParam(required = false) TransactionType type) {
        return ResponseEntity.ok(categoryService.getAllCategory(type));
    }

    @Operation(summary = "Update a category",
            description = "Built-in categories can't be renamed or change their type; only their keywords can be edited. "
                    + "Keywords are cleaned the same way as on create.")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "400", description = "Invalid input, or a keyword shorter than 3 letters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No category with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already taken, or a built-in category would be renamed or retyped",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @Parameter(description = "ID of the category") @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @Operation(summary = "Delete a category",
            description = "Refused for built-in categories, and for any category still used by a transaction or a budget.")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No category with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Built-in category, or still used by transactions or budgets",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID of the category") @PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}