package org.evently.exceptions;

public class InvalidRefundRequestException extends RuntimeException {
    public InvalidRefundRequestException(String message) {
        super(message);
    }
}
