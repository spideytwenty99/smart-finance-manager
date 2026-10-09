package com.saurabh.financemanager.budget;

import com.saurabh.financemanager.budget.dto.BudgetRequest;
import com.saurabh.financemanager.budget.dto.BudgetResponse;
import com.saurabh.financemanager.budget.dto.BudgetUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/budgets")
public class BudgetController {

    private  final BudgetService budgetService;
    public BudgetController(BudgetService budgetService){
        this.budgetService=budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response= budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable UUID id){
        return ResponseEntity.ok(budgetService.getBudget(id));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgetsForMonth(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        return  ResponseEntity.ok(budgetService.getBudgetsForMonth(month));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable UUID id, @Valid @RequestBody BudgetUpdateRequest request){
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id){
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
