package org.example.services;

import org.example.exceptions.*;
import org.example.integrations.PaymentProviderClient;
import org.example.messaging.PaymentEventsPublisher;
import org.example.models.Payment;
import org.example.models.PaymentEvent;
import org.example.repositories.PaymentEventsRepository;
import org.example.repositories.PaymentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentsService {

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private PaymentEventsRepository paymentEventsRepository;

    @Autowired
    private PaymentProviderClient paymentProviderClient;

    @Autowired
    private PaymentEventsPublisher paymentEventsPublisher;

    // --------- helpers ---------

    private void validatePaymentForProcess(Payment payment) {
        if (payment.getOrderId() == null) {
            throw new InvalidPaymentException("OrderId is required");
        }
        if (payment.getUserId() == null) {
            throw new InvalidPaymentException("UserId is required");
        }
        if (payment.getAmount() == null ||
                payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Amount must be greater than 0");
        }
        if (payment.getProvider() == null) {
            throw new InvalidPaymentException("Provider is required");
        }
    }

    private PaymentEvent createEvent(Payment payment, String type, Integer statusCode) {
        PaymentEvent event = new PaymentEvent();
        event.setPayment(payment);
        event.setType(type);
        event.setStatusCode(statusCode);
        return paymentEventsRepository.save(event);
    }

    // --------- use cases ---------

    /**
     * Processar pagamento
     */
    @Transactional
    public Payment processPayment(Payment payment) {
        validatePaymentForProcess(payment);

        payment.setStatus("PENDING");
        Payment savedPending = paymentsRepository.save(payment);
        createEvent(savedPending, "PENDING", null);
        paymentEventsPublisher.publishPaymentEvent("PENDING", savedPending);

        try {
            // chama o provider externo (fake ou real)
            String providerRef = paymentProviderClient.charge(
                    savedPending.getOrderId(),
                    savedPending.getUserId(),
                    savedPending.getAmount(),
                    savedPending.getProvider()
            );

            savedPending.setProviderRef(providerRef);
            savedPending.setStatus("CAPTURED");
            Payment savedCaptured = paymentsRepository.save(savedPending);

            createEvent(savedCaptured, "CAPTURE", 200);
            paymentEventsPublisher.publishPaymentEvent("CAPTURE", savedCaptured);

            return savedCaptured;

        } catch (PaymentRefusedException e) {
            savedPending.setStatus("FAILED");
            Payment failed = paymentsRepository.save(savedPending);

            createEvent(failed, "ERROR", 402);
            paymentEventsPublisher.publishPaymentEvent("FAILED", failed);

            // volta a atirar a exceção para o controller devolver 402
            throw e;
        }
    }

    public Payment getPayment(UUID paymentId) {
        return paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    public List<Payment> getPaymentsByUser(UUID userId) {
        List<Payment> list = paymentsRepository.findByUserId(userId);
        if (list.isEmpty()) {
            throw new UserNotFoundException("User not found or has no payments");
        }
        return list;
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if ("CANCELED".equals(payment.getStatus())) {
            throw new PaymentAlreadyCanceledException("Payment already canceled");
        }
        if ("REFUNDED".equals(payment.getStatus())) {
            throw new InvalidPaymentException("Cannot cancel a refunded payment");
        }

        payment.setStatus("CANCELED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "CANCEL", 200);
        paymentEventsPublisher.publishPaymentEvent("CANCEL", updated);

        return updated;
    }

    @Transactional
    public Payment processRefund(UUID paymentId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!"CAPTURED".equals(payment.getStatus())) {
            throw new InvalidRefundException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus("REFUNDED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "REFUND", 200);
        paymentEventsPublisher.publishPaymentEvent("REFUND", updated);

        return updated;
    }
}