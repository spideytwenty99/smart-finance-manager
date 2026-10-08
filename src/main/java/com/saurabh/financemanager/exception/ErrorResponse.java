package com.saurabh.financemanager.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timeStamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    //This constructor is for errors that aren't about validation, so we don't pass an empty map every time
    public ErrorResponse(int status, String error, String message, String path)
    {
        this(LocalDateTime.now(), status,error,message,path,Map.of());
    }
}
