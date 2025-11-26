package org.example.exceptions;

public class TicketFileNotFoundException extends RuntimeException {
    public TicketFileNotFoundException(String message) {
        super(message);
    }
}
