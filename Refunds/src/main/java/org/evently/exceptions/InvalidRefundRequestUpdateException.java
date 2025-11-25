package org.evently.exceptions;

public class InvalidRefundRequestUpdateException extends RuntimeException {
    public InvalidRefundRequestUpdateException(String message) {
        super(message);
    }
}
