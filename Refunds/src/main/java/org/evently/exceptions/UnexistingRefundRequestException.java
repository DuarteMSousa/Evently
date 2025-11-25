package org.evently.exceptions;

public class UnexistingRefundRequestException extends Exception {
    @Override
    public String getMessage() {
        return "(RefundsService - UnexistingRefundRequest): " +
                "RefundRequest doesn´t exist";
    }
}

