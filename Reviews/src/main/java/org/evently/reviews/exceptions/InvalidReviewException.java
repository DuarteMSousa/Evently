package org.evently.reviews.exceptions;

public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String message) {
        super(message);
    }
}
