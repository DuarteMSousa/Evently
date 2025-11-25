package org.example.integrations;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProviderClient {

    /**
     * Processa o pagamento no provider externo.
     * Deve devolver um transactionId / providerRef.
     *
     * Pode lançar uma PaymentRefusedException.
     */
    String charge(UUID orderId,
                  UUID userId,
                  BigDecimal amount,
                  String provider);
}
