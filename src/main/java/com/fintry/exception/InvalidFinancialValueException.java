package com.fintry.exception;

public class InvalidFinancialValueException extends RuntimeException {
    public InvalidFinancialValueException(String message) {
        super(message);
    }
}
