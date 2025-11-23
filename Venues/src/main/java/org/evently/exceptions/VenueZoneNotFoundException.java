package org.evently.exceptions;

public class VenueZoneNotFoundException extends RuntimeException {
    public VenueZoneNotFoundException(String message) {
        super(message);
    }
}