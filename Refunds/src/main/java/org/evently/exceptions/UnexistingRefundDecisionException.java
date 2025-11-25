package org.evently.exceptions;

public class UnexistingRefundDecisionException extends Exception {
    @Override
    public String getMessage() {
        return "(RefundsService - UnexistingRefundDecision): " +
                "RefundDecision doesn´t exist";
    }
}
