package org.evently.reviews.exceptions;

public class ReviewCommentAlreadyExistsException extends RuntimeException {
    public ReviewCommentAlreadyExistsException(String message) {
        super(message);
    }
}
