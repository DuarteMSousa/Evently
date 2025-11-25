package org.evently.exceptions;

public class RefundDecisionNotFoundException extends RuntimeException {
    public RefundDecisionNotFoundException(String message) {
        super(message);
    }
}
