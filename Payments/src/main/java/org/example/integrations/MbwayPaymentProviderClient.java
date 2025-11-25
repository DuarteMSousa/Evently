package org.example.integrations;

import org.example.exceptions.PaymentRefusedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MbwayPaymentProviderClient implements PaymentProviderClient {

    @Override
    public String charge(UUID orderId,
                         UUID userId,
                         BigDecimal amount,
                         String provider) {

        // Aqui simulamos regras de negócio MBWAY

        // 1) garantir que é mesmo MBWAY (não é obrigatório, mas é seguro)
        if (!"MBWAY".equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("MbwayPaymentProviderClient só deve ser usado com provider = MBWAY");
        }

        // 2) Regras de aceitação (só para teste, podes inventar as tuas):
        //    - Se amount > 100 => recusar
        //    - Senão => aprovar
        if (amount.compareTo(new BigDecimal("100.00")) > 0) {
            throw new PaymentRefusedException("MBWAY: pagamento recusado (valor acima do limite permitido)");
        }

        // 3) Se aprovado, devolvemos um transaction id "MBWAY-<uuid>"
        return "MBWAY-" + UUID.randomUUID();
    }
}
