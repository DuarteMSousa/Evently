package org.evently.exceptions;

public class InvalidRefundRequestMessageException extends RuntimeException {
    public InvalidRefundRequestMessageException(String message) {
        super(message);
    }
}
