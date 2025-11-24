package org.example.exceptions;

public class PaymentRefusedException extends RuntimeException {
    public PaymentRefusedException(String message) {
        super(message);
    }
}
