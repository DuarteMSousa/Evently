package org.example.exceptions;

public class InvalidEventSessionException extends RuntimeException {
    public InvalidEventSessionException(String message) {
        super(message);
    }
}
