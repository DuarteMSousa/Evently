package org.example.exceptions;

public class EventSessionNotFoundException extends RuntimeException {
    public EventSessionNotFoundException(String message) {
        super(message);
    }
}
