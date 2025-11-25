package org.evently.exceptions;

public class RefundRequestMessageNotFoundException extends RuntimeException {
    public RefundRequestMessageNotFoundException(String message) {
        super(message);
    }
}