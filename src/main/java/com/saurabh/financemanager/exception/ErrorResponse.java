package com.saurabh.financemanager.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        @Schema(description = "When the error happened", type = "string", example = "2026-10-12T09:30:12.481")
        LocalDateTime timeStamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Short name of the status", example = "Bad Request")
        String error,

        @Schema(description = "What went wrong", example = "Validation failed")
        String message,

        @Schema(description = "The path that was called", example = "/api/category")
        String path,

        @Schema(description = "Field name to problem, for validation errors; empty otherwise",
                example = "{\"name\": \"Category name is required\"}")
        Map<String, String> validationErrors
) {
    //This constructor is for errors that aren't about validation, so we don't pass an empty map every time
    public ErrorResponse(int status, String error, String message, String path)
    {
        this(LocalDateTime.now(), status,error,message,path,Map.of());
    }
}
