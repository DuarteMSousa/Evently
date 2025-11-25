package org.example.exceptions;

public class InvalidEventUpdateException extends RuntimeException {
    public InvalidEventUpdateException(String message) {
        super(message);
    }
}
