package org.evently.clients;

import org.evently.dtos.externalServices.PaymentStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "payments", path = "/payments")
public interface PaymentsClient {

    @GetMapping("/check-payment-status/{paymentId}")
    public ResponseEntity<PaymentStatusDTO> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId);
}
