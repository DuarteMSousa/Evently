package org.evently.exceptions;

public class VenueAlreadyDeactivatedException extends RuntimeException {
    public VenueAlreadyDeactivatedException(String message) {
        super(message);
    }
}
