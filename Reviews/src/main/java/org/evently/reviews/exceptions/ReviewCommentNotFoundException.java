package org.evently.reviews.exceptions;

public class ReviewCommentNotFoundException extends RuntimeException {
    public ReviewCommentNotFoundException(String message) {
        super(message);
    }
}
