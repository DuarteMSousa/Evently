package org.example.exceptions;

public class TicketStockNotFoundException extends RuntimeException {
    public TicketStockNotFoundException(String message) {
        super(message);
    }
}
