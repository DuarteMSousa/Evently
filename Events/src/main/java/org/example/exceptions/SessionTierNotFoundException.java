package org.example.exceptions;

public class SessionTierNotFoundException extends RuntimeException {
    public SessionTierNotFoundException(String message) {
        super(message);
    }
}
