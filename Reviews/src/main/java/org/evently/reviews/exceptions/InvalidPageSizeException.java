package org.evently.reviews.exceptions;

public class InvalidPageSizeException extends RuntimeException {
    public InvalidPageSizeException(String message) {
        super(message);
    }
}
