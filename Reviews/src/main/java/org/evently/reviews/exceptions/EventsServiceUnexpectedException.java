package org.evently.reviews.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventsServiceUnexpectedException extends Exception {

    private String receivedMessage;

    @Override
    public String getMessage() {
        return String.format("(ReviewsService-EventsService-UnexpectedException): " +
                "Events service replied: %s", receivedMessage);
    }
}
