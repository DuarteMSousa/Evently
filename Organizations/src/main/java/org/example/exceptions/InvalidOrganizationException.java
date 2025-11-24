package org.example.exceptions;

public class InvalidOrganizationException extends RuntimeException {
    public InvalidOrganizationException(String message) {
        super(message);
    }
}
