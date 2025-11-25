package org.example.exceptions;

public class TicketStockAlreadyExistsException extends RuntimeException {
    public TicketStockAlreadyExistsException(String message) {
        super(message);
    }
}
