package com.saurabh.financemanager.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(){
        super("Invalid email or password");
    }
}
