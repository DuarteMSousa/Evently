package org.evently.exceptions;

public class RefundRequestDecisionNotFoundException extends RuntimeException {
    public RefundRequestDecisionNotFoundException(String message) {
        super(message);
    }
}
