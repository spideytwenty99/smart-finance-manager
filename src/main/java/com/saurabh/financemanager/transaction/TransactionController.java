package com.saurabh.financemanager.transaction;

import com.saurabh.financemanager.authentication.CurrentUser;
import com.saurabh.financemanager.exception.ErrorResponse;
import com.saurabh.financemanager.transaction.dto.TransactionRequest;
import com.saurabh.financemanager.transaction.dto.TransactionResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Tag(name = "Transactions", description = "Income and expense entries; categorized automatically when no category is given.")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Create a transaction",
            description = "Send either a categoryId or a transactionType. "
                    + "If a categoryId is given, it is always used and never overridden. "
                    + "Without one, the category is chosen from keywords in the description among categories of the same type; "
                    + "the longest matching keyword wins, and if nothing matches the transaction goes to Uncategorized. "
                    + "If both are sent, the type must match the category's type. "
                    + "autoCategorized in the response is true when the rules chose the category. "
                    + "The date can't be in the future.")
    @ApiResponse(responseCode = "201", description = "Transaction created")
    @ApiResponse(responseCode = "400", description = "Invalid input, neither categoryId nor transactionType given, or type doesn't match the category",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No category with the given categoryId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(CurrentUser.id(jwt),request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a transaction by id")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No transaction with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID of the transaction") @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(CurrentUser.id(jwt),id));
    }

    @Operation(summary = "List transactions",
            description = "Without filters, returns all transactions. Filter by month, or by startDate and endDate together, "
                    + "but not both. Can be combined with categoryId.")
    @ApiResponse(responseCode = "200", description = "List of transactions, possibly empty")
    @ApiResponse(responseCode = "400", description = "Bad date or month format, or an invalid combination of filters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Start of a date range, inclusive. Must be sent together with endDate.", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End of a date range, inclusive. Must be sent together with startDate.", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Month in the format YYYY-MM. Can't be combined with startDate and endDate.", example = "2026-10")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,

            @Parameter(description = "Optional filter: only transactions in this category.")
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(transactionService.getTransactions(CurrentUser.id(jwt),startDate, endDate, month, categoryId));
    }

    @Operation(summary = "Update a transaction",
            description = "Replaces all fields. The same category rules as on create apply. "
                    + "Sending your own categoryId for a guessed transaction sets autoCategorized to false.")
    @ApiResponse(responseCode = "200", description = "Transaction updated")
    @ApiResponse(responseCode = "400", description = "Invalid input, neither categoryId nor transactionType given, or type doesn't match the category",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No transaction with this id, or no category with the given categoryId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID of the transaction") @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(CurrentUser.id(jwt),id, request));
    }

    @Operation(summary = "Delete a transaction")
    @ApiResponse(responseCode = "204", description = "Transaction deleted")
    @ApiResponse(responseCode = "400", description = "The id is not a valid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No transaction with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID of the transaction") @PathVariable UUID id) {
        transactionService.deleteTransaction(CurrentUser.id(jwt),id);
        return ResponseEntity.noContent().build();
    }
}