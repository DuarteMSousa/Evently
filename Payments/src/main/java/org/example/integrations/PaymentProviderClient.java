package org.example.integrations;

import org.example.models.Payment;

public interface PaymentProviderClient {

    /**
     * Creates a payment order int the provider
     * Should update the payment and retrieve the url for the user approve the payment
     */
    String createPaymentOrder(Payment payment);

    /**
     * Captures the payment after user approval
     */
    void capturePayment(String providerRef);
}
