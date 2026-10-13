package com.fintry.exception;

public class RegistrationConflictException extends BusinessRuleException {
    private final String correlationId;
    public RegistrationConflictException(String correlationId) {
        super("Registration could not be completed");
        this.correlationId = correlationId;
    }
    public String getCorrelationId() { return correlationId; }
}
