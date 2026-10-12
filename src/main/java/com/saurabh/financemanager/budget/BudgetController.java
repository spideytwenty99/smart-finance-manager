package com.saurabh.financemanager.budget;

import com.saurabh.financemanager.budget.dto.BudgetRequest;
import com.saurabh.financemanager.budget.dto.BudgetResponse;
import com.saurabh.financemanager.budget.dto.BudgetUpdateRequest;
import com.saurabh.financemanager.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Tag(name = "Budgets", description = "Monthly spending limits per expense category.")
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Operation(summary = "Create a budget",
            description = "Only for expense categories. Each category can have one budget per month.")
    @ApiResponse(responseCode = "201", description = "Budget created")
    @ApiResponse(responseCode = "400", description = "Invalid input, or the category is an income category",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No category with the given categoryId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "This category already has a budget for this month",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a budget by id")
    @ApiResponse(responseCode = "200", description = "Budget found")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No budget with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudget(
            @Parameter(description = "ID of the budget") @PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.getBudget(id));
    }

    @Operation(summary = "List budgets for a month",
            description = "The month is required.")
    @ApiResponse(responseCode = "200", description = "List of budgets, possibly empty")
    @ApiResponse(responseCode = "400", description = "Month missing or not in the format YYYY-MM",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgetsForMonth(
            @Parameter(description = "Month in the format YYYY-MM. Required.", example = "2026-10")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(month));
    }

    @Operation(summary = "Update a budget's limit",
            description = "Only the limit can change. To use a different category or month, delete this budget and create a new one.")
    @ApiResponse(responseCode = "200", description = "Budget updated")
    @ApiResponse(responseCode = "400", description = "Invalid limit",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No budget with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @Parameter(description = "ID of the budget") @PathVariable UUID id,
            @Valid @RequestBody BudgetUpdateRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @Operation(summary = "Delete a budget",
            description = "Transactions are not affected.")
    @ApiResponse(responseCode = "204", description = "Budget deleted")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No budget with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @Parameter(description = "ID of the budget") @PathVariable UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}