package org.evently.reviews.exceptions;

public class InvalidReviewCommentException extends RuntimeException {
    public InvalidReviewCommentException(String message) {
        super(message);
    }
}
