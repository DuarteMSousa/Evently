package org.example.integrations;

import org.example.exceptions.PaymentRefusedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

//@Component
public class FakePaymentProviderClient implements PaymentProviderClient {

    @Override
    public String charge(UUID orderId,
                         UUID userId,
                         BigDecimal amount,
                         String provider) {

        // Regra fake: se o valor < 5, recusar
        if (amount.compareTo(new BigDecimal("5.00")) < 0) {
            throw new PaymentRefusedException("Provider refused payment (amount too low)");
        }

        // Gera um “transaction id” fake
        return "TX-" + UUID.randomUUID();
    }
}
