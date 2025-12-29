package org.example.exceptions;

public class InvalidSessionTierException extends RuntimeException {
    public InvalidSessionTierException(String message) {
        super(message);
    }
}
