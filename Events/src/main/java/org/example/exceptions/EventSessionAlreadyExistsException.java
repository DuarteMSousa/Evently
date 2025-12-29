package org.example.exceptions;

public class EventSessionAlreadyExistsException extends RuntimeException {
    public EventSessionAlreadyExistsException(String message) {
        super(message);
    }
}
