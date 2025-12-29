package org.example.exceptions;

public class SessionTierAlreadyExistsException extends RuntimeException {
    public SessionTierAlreadyExistsException(String message) {
        super(message);
    }
}
