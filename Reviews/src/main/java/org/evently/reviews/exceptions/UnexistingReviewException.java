package org.evently.reviews.exceptions;

public class UnexistingReviewException extends Exception {
    @Override
    public String getMessage() {
        return "(ReviewsService - UnexistingReview): " +
                "Review doesn´t exist";
    }
}
