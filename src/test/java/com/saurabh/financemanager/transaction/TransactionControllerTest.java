package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldReturn201ForValidPost() throws Exception {
        TransactionResponse response = new TransactionResponse();
        response.setId(UUID.randomUUID());
        response.setAmount(new BigDecimal("23.45"));
        response.setDescription("REWE Markt Frankfurt");
        response.setTransactionDate(LocalDate.of(2026, 10, 11));
        response.setCategoryName("Groceries");
        response.setTransactionType(TransactionType.EXPENSE);
        response.setAutoCategorized(true);

        when(transactionService.createTransaction(any())).thenReturn(response);

        String body = """
                {
                  "amount": 23.45,
                  "description": "REWE Markt Frankfurt",
                  "transactionDate": "2026-10-11",
                  "transactionType": "EXPENSE"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.autoCategorized").value(true))
                .andExpect(jsonPath("$.amount").value(23.45));
    }

    @Test
    void shouldReturn400WithFieldErrorsForInvalidBody() throws Exception {
        // negative amount, and transactionDate is missing
        String body = """
                {
                  "amount": -5,
                  "description": "Broken request",
                  "transactionType": "EXPENSE"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.amount").value("Amount must be greater than 0"))
                .andExpect(jsonPath("$.validationErrors.transactionDate").exists())
                .andExpect(jsonPath("$.path").value("/api/transactions"));

        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void shouldReturn404InErrorResponseShapeForMissingId() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.getTransaction(id))
                .thenThrow(new ResourceNotFoundException("Transaction with id " + id + " not found"));

        mockMvc.perform(get("/api/transactions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction with id " + id + " not found"))
                .andExpect(jsonPath("$.path").value("/api/transactions/" + id))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void shouldReturn400ForInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/transactions").param("month", "October"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(transactionService, never()).getTransactions(any(), any(), any(), any());
    }
}