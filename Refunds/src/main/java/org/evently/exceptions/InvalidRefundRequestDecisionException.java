package org.evently.exceptions;

public class InvalidRefundRequestDecisionException extends RuntimeException {
    public InvalidRefundRequestDecisionException(String message) {
        super(message);
    }
}
