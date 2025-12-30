package org.example.exceptions;

public class InvalidTicketReservationException extends RuntimeException {
    public InvalidTicketReservationException(String message) {
        super(message);
    }
}
