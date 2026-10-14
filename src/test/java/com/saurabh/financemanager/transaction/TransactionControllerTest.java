package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.authentication.CustomUserDetailsService;
import com.saurabh.financemanager.authentication.JsonAuthenticationEntryPoint;
import com.saurabh.financemanager.config.SecurityConfig;
import com.saurabh.financemanager.exception.ResourceNotFoundException;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0cy1vbmx5LTEyMzQ1Ng==",
        "jwt.issuer=smart-finance-manager"
})
class TransactionControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    // SecurityConfig needs this to build the AuthenticationManager
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // A logged-in user whose token subject is USER_ID
    private static RequestPostProcessor loggedIn() {
        return jwt().jwt(token -> token.subject(USER_ID.toString()));
    }

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

        when(transactionService.createTransaction(eq(USER_ID), any())).thenReturn(response);

        String body = """
                {
                  "amount": 23.45,
                  "description": "REWE Markt Frankfurt",
                  "transactionDate": "2026-10-11",
                  "transactionType": "EXPENSE"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.autoCategorized").value(true))
                .andExpect(jsonPath("$.amount").value(23.45));
    }

    @Test
    void shouldReturn400WithFieldErrorsForInvalidBody() throws Exception {
        String body = """
                {
                  "amount": -5,
                  "description": "Broken request",
                  "transactionType": "EXPENSE"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .with(loggedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.amount").value("Amount must be greater than 0"))
                .andExpect(jsonPath("$.validationErrors.transactionDate").exists())
                .andExpect(jsonPath("$.path").value("/api/transactions"));

        verify(transactionService, never()).createTransaction(any(), any());
    }

    @Test
    void shouldReturn404InErrorResponseShapeForMissingId() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.getTransaction(USER_ID, id))
                .thenThrow(new ResourceNotFoundException("Transaction with id " + id + " not found"));

        mockMvc.perform(get("/api/transactions/{id}", id).with(loggedIn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction with id " + id + " not found"))
                .andExpect(jsonPath("$.path").value("/api/transactions/" + id))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void shouldReturn400ForInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/transactions").param("month", "October").with(loggedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(transactionService, never()).getTransactions(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturn401InErrorResponseShapeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Missing or invalid token"))
                .andExpect(jsonPath("$.path").value("/api/transactions"));

        verify(transactionService, never()).getTransactions(any(), any(), any(), any(), any());
    }
}