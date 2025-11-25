package org.evently.reviews.exceptions;

public class InvalidReviewCommentUpdateException extends RuntimeException {
    public InvalidReviewCommentUpdateException(String message) {
        super(message);
    }
}
