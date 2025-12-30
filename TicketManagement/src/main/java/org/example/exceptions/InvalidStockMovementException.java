package org.example.exceptions;

public class InvalidStockMovementException extends RuntimeException {
    public InvalidStockMovementException(String message) {
        super(message);
    }
}
