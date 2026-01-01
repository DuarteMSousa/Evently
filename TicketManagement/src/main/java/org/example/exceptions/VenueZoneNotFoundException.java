package org.example.exceptions;

public class VenueZoneNotFoundException extends RuntimeException {
    public VenueZoneNotFoundException(String message) {
        super(message);
    }
}
