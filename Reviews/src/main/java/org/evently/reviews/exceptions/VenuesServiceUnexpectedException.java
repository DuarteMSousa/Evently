package org.evently.reviews.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VenuesServiceUnexpectedException extends Exception {

    private String receivedMessage;

    @Override
    public String getMessage() {
        return String.format("(ReviewsService-VenuesService-UnexpectedException): " +
                "Venues service replied: %s", receivedMessage);
    }
}
