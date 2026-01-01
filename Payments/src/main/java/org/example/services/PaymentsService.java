package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.*;
import org.example.integrations.PaymentProviderClient;
import org.example.messaging.PaymentEventsPublisher;
import org.example.models.Payment;
import org.example.models.PaymentEvent;
import org.example.repositories.PaymentEventsRepository;
import org.example.repositories.PaymentsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentsService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentsService.class);

    private static final Marker PAY_VALIDATE = MarkerFactory.getMarker("PAYMENT_VALIDATE");
    private static final Marker PAY_PROCESS = MarkerFactory.getMarker("PAYMENT_PROCESS");
    private static final Marker PAY_CAPTURE = MarkerFactory.getMarker("PAYMENT_CAPTURE");
    private static final Marker PAY_CANCEL = MarkerFactory.getMarker("PAYMENT_CANCEL");
    private static final Marker PAY_REFUND = MarkerFactory.getMarker("PAYMENT_REFUND");
    private static final Marker PAY_GET = MarkerFactory.getMarker("PAYMENT_GET");
    private static final Marker PAY_LIST = MarkerFactory.getMarker("PAYMENT_LIST");
    private static final Marker PAY_PROVIDER = MarkerFactory.getMarker("PAYMENT_PROVIDER");
    private static final Marker PAY_EVENT = MarkerFactory.getMarker("PAYMENT_EVENT");

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private PaymentEventsRepository paymentEventsRepository;

    @Autowired
    private PaymentEventsPublisher paymentEventsPublisher;

    @Autowired
    private PaymentProviderClient paymentProviderClient;

    /**
     * Validates a payment payload before starting the payment process.
     *
     * @param payment payment to validate
     * @throws InvalidPaymentException if any required field is missing/invalid or provider is not supported
     */
    private void validatePaymentForProcess(Payment payment) {
        logger.debug(PAY_VALIDATE,
                "Validating payment for process (paymentId={}, orderId={}, userId={}, provider={}, amount={})",
                payment != null ? payment.getId() : null,
                payment != null ? payment.getOrderId() : null,
                payment != null ? payment.getUserId() : null,
                payment != null ? payment.getProvider() : null,
                payment != null ? payment.getAmount() : null
        );

        if (payment.getOrderId() == null) {
            logger.warn(PAY_VALIDATE, "Missing orderId");
            throw new InvalidPaymentException("OrderId is required");
        }
        if (payment.getUserId() == null) {
            logger.warn(PAY_VALIDATE, "Missing userId (orderId={})", payment.getOrderId());
            throw new InvalidPaymentException("UserId is required");
        }
        if (payment.getAmount() <= 0) {
            logger.warn(PAY_VALIDATE, "Invalid amount={} (orderId={}, userId={})",
                    payment.getAmount(), payment.getOrderId(), payment.getUserId());
            throw new InvalidPaymentException("Amount must be greater than 0");
        }
        if (payment.getProvider() == null) {
            logger.warn(PAY_VALIDATE, "Missing provider (orderId={}, userId={})",
                    payment.getOrderId(), payment.getUserId());
            throw new InvalidPaymentException("Provider is required");
        }
        if (!"PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            logger.warn(PAY_VALIDATE, "Unsupported provider={} (orderId={})", payment.getProvider(), payment.getOrderId());
            throw new InvalidPaymentException("Only PAYPAL provider is supported");
        }
    }

    /**
     * Persists a payment event (audit/history) for a given payment.
     *
     * @param payment payment that originated the event
     * @param type event type (e.g., PENDING, CAPTURE, CANCEL, REFUND, ERROR)
     * @param statusCode optional status code associated with the event (e.g., 200, 402)
     * @return persisted payment event
     */
    private PaymentEvent createEvent(Payment payment, String type, Integer statusCode) {
        PaymentEvent event = new PaymentEvent();
        event.setPayment(payment);
        event.setType(type);
        event.setStatusCode(statusCode);

        PaymentEvent saved = paymentEventsRepository.save(event);

        logger.debug(PAY_EVENT, "Payment event persisted (paymentId={}, eventId={}, type={}, statusCode={})",
                payment.getId(), saved.getId(), type, statusCode);

        return saved;
    }

    /**
     * Publishes a payment lifecycle event to the messaging layer.
     *
     * @param type event type (e.g., PENDING, CAPTURE, CANCEL, REFUND, FAILED)
     * @param payment payment associated with the event
     */
    private void publishEvent(String type, Payment payment) {
        logger.info(PAY_EVENT, "Publishing payment event (type={}, paymentId={}, orderId={}, status={})",
                type, payment.getId(), payment.getOrderId(), payment.getStatus());

        paymentEventsPublisher.publishPaymentEvent(type, payment);

        logger.debug(PAY_EVENT, "Payment event published (type={}, paymentId={})", type, payment.getId());
    }

    /**
     * Creates and initiates a payment through the configured payment provider.
     *
     *
     * @param payment payment request payload
     * @return persisted payment with updated provider reference
     * @throws InvalidPaymentException if payload is invalid or provider not supported
     * @throws PaymentRefusedException if provider refuses the payment
     * @throws RuntimeException for unexpected errors during persistence/provider communication
     */
    @Transactional
    public Payment processPayment(Payment payment) {
        logger.info(PAY_PROCESS, "Process payment requested (orderId={}, userId={}, provider={}, amount={})",
                payment != null ? payment.getOrderId() : null,
                payment != null ? payment.getUserId() : null,
                payment != null ? payment.getProvider() : null,
                payment != null ? payment.getAmount() : null
        );

        validatePaymentForProcess(payment);

        payment.setStatus("PENDING");
        Payment saved = paymentsRepository.save(payment);

        logger.info(PAY_PROCESS, "Payment saved as PENDING (paymentId={}, orderId={})",
                saved.getId(), saved.getOrderId());

        try {
            logger.info(PAY_PROVIDER, "Creating PayPal order (paymentId={}, orderId={})",
                    saved.getId(), saved.getOrderId());

            paymentProviderClient.createPaymentOrder(saved);

            logger.info(PAY_PROVIDER, "PayPal order created (paymentId={}, providerRef={})",
                    saved.getId(), saved.getProviderRef());

            Payment updated = paymentsRepository.save(saved);

            createEvent(updated, "PENDING", null);
            publishEvent("PENDING", updated);

            logger.info(PAY_PROCESS, "Process payment completed (paymentId={}, status={}, providerRef={})",
                    updated.getId(), updated.getStatus(), updated.getProviderRef());

            return updated;

        } catch (PaymentRefusedException e) {
            saved.setStatus("FAILED");
            Payment failed = paymentsRepository.save(saved);

            createEvent(failed, "ERROR", 402);
            publishEvent("FAILED", failed);

            logger.warn(PAY_PROCESS, "Payment refused by provider (paymentId={}, orderId={}, providerRef={})",
                    failed.getId(), failed.getOrderId(), failed.getProviderRef(), e);

            throw e;

        } catch (RuntimeException e) {
            logger.error(PAY_PROCESS, "Unexpected error while processing payment (paymentId={}, orderId={})",
                    saved.getId(), saved.getOrderId(), e);
            throw e;
        }
    }

    /**
     * Captures a PayPal payment after provider callback, using the provider reference (token).
     *
     *
     * @param providerRef provider reference (e.g. PayPal token)
     * @return updated payment
     * @throws PaymentNotFoundException if no payment exists for the providerRef
     * @throws InvalidPaymentException if provider is not PAYPAL or status is not PENDING
     * @throws RuntimeException if provider capture fails unexpectedly
     */
    @Transactional
    public Payment capturePaypalPayment(String providerRef) {
        logger.info(PAY_CAPTURE, "Capture PayPal payment requested (providerRef={})", providerRef);

        Payment payment = paymentsRepository.findByProviderRef(providerRef)
                .orElseThrow(() -> {
                    logger.warn(PAY_CAPTURE, "Payment not found for providerRef={}", providerRef);
                    return new PaymentNotFoundException("Payment not found for providerRef");
                });

        if (!"PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            logger.warn(PAY_CAPTURE, "Payment is not PAYPAL (paymentId={}, provider={})",
                    payment.getId(), payment.getProvider());
            throw new InvalidPaymentException("Payment is not PAYPAL");
        }

        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            logger.warn(PAY_CAPTURE, "Only PENDING can be captured (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());
            throw new InvalidPaymentException("Only PENDING PAYPAL payments can be captured");
        }

        logger.info(PAY_PROVIDER, "Capturing PayPal payment (paymentId={}, providerRef={})",
                payment.getId(), providerRef);

        paymentProviderClient.capturePayment(providerRef);

        payment.setStatus("CAPTURED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "CAPTURE", 200);
        publishEvent("CAPTURE", updated);

        logger.info(PAY_CAPTURE, "Capture completed (paymentId={}, status={})",
                updated.getId(), updated.getStatus());

        return updated;
    }

    /**
     * Retrieves a payment by its unique identifier.
     *
     * @param paymentId payment identifier
     * @return found payment
     * @throws PaymentNotFoundException if the payment does not exist
     */
    public Payment getPayment(UUID paymentId) {
        logger.debug(PAY_GET, "Get payment requested (paymentId={})", paymentId);

        return paymentsRepository.findById(paymentId)
                .orElseThrow(() -> {
                    logger.warn(PAY_GET, "Payment not found (paymentId={})", paymentId);
                    return new PaymentNotFoundException("Payment not found");
                });
    }

    /**
     * Retrieves all payments associated with a given user.
     *
     *
     * @param userId user identifier
     * @return list of payments for the user
     * @throws UserNotFoundException if no payments are found for the given userId
     */
    public List<Payment> getPaymentsByUser(UUID userId) {
        logger.debug(PAY_LIST, "Get payments by user requested (userId={})", userId);

        List<Payment> list = paymentsRepository.findByUserId(userId);

        if (list.isEmpty()) {
            logger.warn(PAY_LIST, "No payments found for user (userId={})", userId);
            throw new UserNotFoundException("User not found or has no payments");
        }

        logger.debug(PAY_LIST, "Get payments by user completed (userId={}, results={})", userId, list.size());
        return list;
    }

    /**
     * Cancels a payment.
     *
     *
     * @param paymentId payment identifier
     * @return updated payment
     * @throws PaymentNotFoundException if the payment does not exist
     * @throws PaymentAlreadyCanceledException if the payment is already canceled
     * @throws InvalidPaymentException if the payment is refunded (invalid cancel state)
     */
    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        logger.info(PAY_CANCEL, "Cancel payment requested (paymentId={})", paymentId);

        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> {
                    logger.warn(PAY_CANCEL, "Payment not found (paymentId={})", paymentId);
                    return new PaymentNotFoundException("Payment not found");
                });

        if ("CANCELED".equalsIgnoreCase(payment.getStatus())) {
            logger.warn(PAY_CANCEL, "Payment already canceled (paymentId={})", paymentId);
            throw new PaymentAlreadyCanceledException("Payment already canceled");
        }
        if ("REFUNDED".equalsIgnoreCase(payment.getStatus())) {
            logger.warn(PAY_CANCEL, "Cannot cancel refunded payment (paymentId={})", paymentId);
            throw new InvalidPaymentException("Cannot cancel a refunded payment");
        }

        payment.setStatus("CANCELED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "CANCEL", 200);
        publishEvent("CANCEL", updated);

        logger.info(PAY_CANCEL, "Cancel completed (paymentId={}, status={})", updated.getId(), updated.getStatus());

        return updated;
    }

    /**
     * Processes a refund for a payment.
     *
     *
     * @param paymentId payment identifier
     * @return updated payment
     * @throws PaymentNotFoundException if the payment does not exist
     * @throws InvalidRefundException if the payment status is not CAPTURED
     */
    @Transactional
    public Payment processRefund(UUID paymentId) {
        logger.info(PAY_REFUND, "Refund payment requested (paymentId={})", paymentId);

        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> {
                    logger.warn(PAY_REFUND, "Payment not found (paymentId={})", paymentId);
                    return new PaymentNotFoundException("Payment not found");
                });

        if (!"CAPTURED".equalsIgnoreCase(payment.getStatus())) {
            logger.warn(PAY_REFUND, "Only CAPTURED payments can be refunded (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());
            throw new InvalidRefundException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus("REFUNDED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "REFUND", 200);
        publishEvent("REFUND", updated);

        logger.info(PAY_REFUND, "Refund completed (paymentId={}, status={})",
                updated.getId(), updated.getStatus());

        return updated;
    }

    @Transactional
    public void onRefundProcessed(UUID paymentId, float amount, UUID refundId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!"CAPTURED".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        payment.setStatus("REFUNDED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "REFUND", 200);
        publishEvent("REFUND", updated);
    }

    @Transactional
    public void onRefundFailed(UUID paymentId, UUID refundId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus("REFUND_FAILED");
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, "REFUND_FAILED", 409);
        publishEvent("REFUND_FAILED", updated);
    }
}
