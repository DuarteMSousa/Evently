package org.example.services;

import org.example.exceptions.*;
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

        // estado inicial
        payment.setStatus("PENDING");

        // aqui seria a chamada real ao provider (Stripe, PayPal, etc.)
        // vamos simular:
        boolean approved = payment.getAmount().compareTo(new BigDecimal("5.00")) >= 0;

        if (!approved) {
            payment.setStatus("FAILED");
            Payment saved = paymentsRepository.save(payment);
            createEvent(saved, "ERROR", 402);
            throw new PaymentRefusedException("Payment refused by provider");
        }

        payment.setStatus("CAPTURED");
        Payment savedPayment = paymentsRepository.save(payment);
        createEvent(savedPayment, "CAPTURE", 200);

        return savedPayment;
    }

    /**
     * Obter pagamento por id
     */
    public Payment getPayment(UUID paymentId) {
        return paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    /**
     * Obter pagamentos de um utilizador
     */
    public List<Payment> getPaymentsByUser(UUID userId) {
        List<Payment> list = paymentsRepository.findByUserId(userId);
        if (list.isEmpty()) {
            throw new UserNotFoundException("User not found or has no payments");
        }
        return list;
    }

    /**
     * Cancelar pagamento
     */
    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.getStatus().equals("CANCELED")) {
            throw new PaymentAlreadyCanceledException("Payment already canceled");
        }
        if (payment.getStatus().equals("REFUNDED")) {
            throw new InvalidPaymentException("Cannot cancel a refunded payment");
        }

        payment.setStatus("CANCELED");
        Payment updated = paymentsRepository.save(payment);
        createEvent(updated, "CANCEL", 200);

        return updated;
    }

    /**
     * Processar reembolso
     */
    @Transactional
    public Payment processRefund(UUID paymentId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getStatus().equals("CAPTURED")) {
            throw new InvalidRefundException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus("REFUNDED");
        Payment updated = paymentsRepository.save(payment);
        createEvent(updated, "REFUND", 200);

        return updated;
    }
}