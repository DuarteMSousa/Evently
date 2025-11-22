package org.evently.reviews.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrganizationsServiceUnexpectedException extends Exception {

    private String receivedMessage;

    @Override
    public String getMessage() {
        return String.format("(ReviewsService-OrganizationsService-UnexpectedException): " +
                "Organizations service replied: %s", receivedMessage);
    }
}
