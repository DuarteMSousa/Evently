package org.example.exceptions;

public class PaymentAlreadyCanceledException extends RuntimeException {
    public PaymentAlreadyCanceledException(String message) {
        super(message);
    }
}
