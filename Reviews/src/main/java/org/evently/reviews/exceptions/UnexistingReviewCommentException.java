package org.evently.reviews.exceptions;

public class UnexistingReviewCommentException extends Exception {
    @Override
    public String getMessage() {
        return "(ReviewsService - UnexistingReviewComment): " +
                "Review Comment doesn´t exist";
    }
}
