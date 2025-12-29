package org.example.exceptions;

public class InvalidEventSessionUpdateException extends RuntimeException {
    public InvalidEventSessionUpdateException(String message) {
        super(message);
    }
}
