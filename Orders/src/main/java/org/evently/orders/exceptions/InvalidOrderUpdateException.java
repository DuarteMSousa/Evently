package org.evently.orders.exceptions;

public class InvalidOrderUpdateException extends RuntimeException {
    public InvalidOrderUpdateException(String message) {
        super(message);
    }
}
