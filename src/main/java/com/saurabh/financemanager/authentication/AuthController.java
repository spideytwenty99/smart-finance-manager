package com.saurabh.financemanager.authentication;

import com.saurabh.financemanager.authentication.dto.AuthResponse;
import com.saurabh.financemanager.authentication.dto.LoginRequest;
import com.saurabh.financemanager.authentication.dto.RegisterRequest;
import com.saurabh.financemanager.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register and log in to get a JWT for the other endpoints.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new account",
            description = "Creates the account with its own copy of the default categories, "
                    + "and logs the user in right away. The email is stored lowercase; "
                    + "the password must be 8 to 72 characters.")
    @ApiResponse(responseCode = "201", description = "Account created, token returned")
    @ApiResponse(responseCode = "400", description = "Invalid email or password format",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "An account with this email already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Log in",
            description = "Returns a JWT. Send it as 'Authorization: Bearer <token>' on all other requests.")
    @ApiResponse(responseCode = "200", description = "Logged in, token returned")
    @ApiResponse(responseCode = "400", description = "Email or password missing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}