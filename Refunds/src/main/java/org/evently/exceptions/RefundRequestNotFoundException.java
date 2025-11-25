package org.evently.exceptions;

public class RefundRequestNotFoundException extends RuntimeException {
    public RefundRequestNotFoundException(String message) {
        super(message);
    }
}
