package org.evently.exceptions;

public class UnexistingRefundRequestMessageException extends Exception {
    @Override
    public String getMessage() {
        return "(RefundsService - UnexistingRefundRequestMessage): " +
                "RefundRequestMessage doesn´t exist";
    }
}
