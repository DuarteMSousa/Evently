package org.example.integrations;

import org.example.models.Payment;

public interface PaymentProviderClient {

    /**
     * Cria uma ordem de pagamento no provider (ex.: PayPal)
     * Deve atualizar o Payment (providerRef, etc.)
     * e devolver o URL para o utilizador aprovar o pagamento.
     */
    String createPaymentOrder(Payment payment);

    /**
     * Captura/finaliza o pagamento depois do utilizador aprovar.
     */
    void capturePayment(String providerRef);
}
