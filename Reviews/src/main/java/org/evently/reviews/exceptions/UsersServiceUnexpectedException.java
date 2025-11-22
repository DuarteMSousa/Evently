package org.evently.reviews.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UsersServiceUnexpectedException extends Exception {

    private String receivedMessage;

    @Override
    public String getMessage() {
        return String.format("(ReviewsService-UsersService-UnexpectedException): " +
                "Users service replied: %s", receivedMessage);
    }
}
