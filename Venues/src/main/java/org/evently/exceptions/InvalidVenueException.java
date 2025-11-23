package org.evently.exceptions;

public class InvalidVenueException extends RuntimeException {
    public InvalidVenueException(String message) {
        super(message);
    }
}
