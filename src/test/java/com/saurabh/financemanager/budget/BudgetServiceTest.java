package com.saurabh.financemanager.budget;


import com.saurabh.financemanager.budget.dto.BudgetRequest;
import com.saurabh.financemanager.budget.dto.BudgetResponse;
import com.saurabh.financemanager.budget.dto.BudgetUpdateRequest;
import com.saurabh.financemanager.category.Category;
import com.saurabh.financemanager.category.CategoryRepository;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.transaction.Transaction;
import com.saurabh.financemanager.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void  shouldThrowWhenBudgetAlreadyExistsForMonth(){
        //arrange: Groceries already has an October budget
        Category groceries=category("Groceires", TransactionType.EXPENSE);
        when(categoryRepository.findById(any())).thenReturn(Optional.of(groceries));
        when(budgetRepository.existsByCategoryIdAndMonth(any(), any())).thenReturn(true);

        //act + assert
        assertThrows(DuplicateResourceException.class,
                ()->budgetService.createBudget(request("300.00")));
        verify(budgetRepository,never()).save(any());
    }

    @Test
    void shouldRejectBudgetForIncomeCategory(){
        //arange: saraly is an income category
        Category salary=category("Salary", TransactionType.INCOME);
        when(categoryRepository.findById(any())).thenReturn(Optional.of(salary));

        //act + assert
        assertThrows(IllegalArgumentException.class,
                ()->budgetService.createBudget(request("1000.00")));

        verify(budgetRepository,never()).save(any());
    }
    @Test
    void shouldChangeOnlyLimitOnUpdate(){
        //arrange: an existing Groceries budget for october with limit 300
        Category groceries=category("Groceries", TransactionType.EXPENSE);
        Budget existing=new Budget();
        existing.setCategory(groceries);
        existing.setMonth(LocalDate.of(2026, 10, 1));
        existing.setAmountLimit(new BigDecimal("300.00"));

        UUID budgetId= UUID.randomUUID();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(call->call.getArgument(0));

        BudgetUpdateRequest update=new BudgetUpdateRequest();
        update.setAmountLimit(new BigDecimal("350.00"));

        //act
        BudgetResponse response=budgetService.updateBudget(budgetId, update);

        //assert: the limit changed
        assertEquals(new BigDecimal("350.00"),response.getAmountLimit() );
        // ...and everything else stayed the same
        assertEquals("Groceries", response.getCategoryName());
        assertEquals(YearMonth.of(2026, 10), response.getMonth());
        assertEquals(LocalDate.of(2026, 10, 1), existing.getMonth());
        assertEquals(groceries, existing.getCategory());
    }

    //helpers
    private  Category category(String name, TransactionType type){
        Category category=new Category();
        category.setName(name);
        category.setTransactionType(type);

        return category;
    }

    private BudgetRequest request(String limit){
        BudgetRequest request=new BudgetRequest();
        request.setCategoryId(UUID.randomUUID());
        request.setMonth(YearMonth.of(2026, 10));
        request.setAmountLimit(new BigDecimal(limit));
        return request;
    }
}
