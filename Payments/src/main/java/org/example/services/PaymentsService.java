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
    private PaymentEventsPublisher paymentEventsPublisher;

    @Autowired
    private PaymentProviderClient paymentProviderClient; // PayPal

    // ---------- helpers ----------

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
        if (!"PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            throw new InvalidPaymentException("Only PAYPAL provider is supported");
        }
    }

    private PaymentEvent createEvent(Payment payment, String type, Integer statusCode) {
        PaymentEvent event = new PaymentEvent();
        event.setPayment(payment);
        event.setType(type);
        event.setStatusCode(statusCode);
        return paymentEventsRepository.save(event);
    }

    // ---------- use cases ----------

    @Transactional
    public Payment processPayment(Payment payment) {
        validatePaymentForProcess(payment);

        // estado inicial
        payment.setStatus("PENDING");
        Payment saved = paymentsRepository.save(payment);

        try {
            // cria ordem no PayPal e preenche providerRef
            paymentProviderClient.createPaymentOrder(saved);

            Payment updated = paymentsRepository.save(saved);

            createEvent(updated, "PENDING", null);
            paymentEventsPublisher.publishPaymentEvent("PENDING", updated);

            return updated;

        } catch (PaymentRefusedException e) {
            saved.setStatus("FAILED");
            Payment failed = paymentsRepository.save(saved);

            createEvent(failed, "ERROR", 402);
            paymentEventsPublisher.publishPaymentEvent("FAILED", failed);

            throw e;
        }
    }

    @Transactional
    public Payment capturePaypalPayment(String providerRef) {
        Payment payment = paymentsRepository.findByProviderRef(providerRef)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for providerRef"));

        if (!"PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            throw new InvalidPaymentException("Payment is not PAYPAL");
        }

        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidPaymentException("Only PENDING PAYPAL payments can be captured");
        }

        paymentProviderClient.capturePayment(providerRef);

        payment.setStatus("CAPTURED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "CAPTURE", 200);
        paymentEventsPublisher.publishPaymentEvent("CAPTURE", updated);

        return updated;
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

        if ("CANCELED".equalsIgnoreCase(payment.getStatus())) {
            throw new PaymentAlreadyCanceledException("Payment already canceled");
        }
        if ("REFUNDED".equalsIgnoreCase(payment.getStatus())) {
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

        if (!"CAPTURED".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidRefundException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus("REFUNDED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "REFUND", 200);
        paymentEventsPublisher.publishPaymentEvent("REFUND", updated);

        return updated;
    }
}