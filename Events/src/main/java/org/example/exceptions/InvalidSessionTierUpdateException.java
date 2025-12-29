package org.example.exceptions;

public class InvalidSessionTierUpdateException extends RuntimeException {
    public InvalidSessionTierUpdateException(String message) {
        super(message);
    }
}
