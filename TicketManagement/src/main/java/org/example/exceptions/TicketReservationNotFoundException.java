package org.example.exceptions;

public class TicketReservationNotFoundException extends RuntimeException {
    public TicketReservationNotFoundException(String message) {
        super(message);
    }
}
