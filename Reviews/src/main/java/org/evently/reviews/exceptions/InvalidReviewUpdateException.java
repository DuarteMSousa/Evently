package org.evently.reviews.exceptions;

public class InvalidReviewUpdateException extends RuntimeException {
    public InvalidReviewUpdateException(String message) {
        super(message);
    }
}
