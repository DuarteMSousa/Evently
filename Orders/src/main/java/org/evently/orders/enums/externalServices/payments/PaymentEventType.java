package org.evently.orders.enums.externalServices.payments;

public enum PaymentEventType {
    PENDING,
    CAPTURED,
    CANCEL,
    REFUND,
    REFUND_FAILED,
    FAILED,
    ERROR
}
