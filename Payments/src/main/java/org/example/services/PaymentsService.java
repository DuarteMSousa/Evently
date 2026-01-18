package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.var;
import org.example.clients.OrdersClient;
import org.example.dtos.externalServices.OrderDTO;
import org.example.enums.PaymentEventType;
import org.example.enums.PaymentProvider;
import org.example.enums.PaymentStatus;
import org.example.exceptions.*;
import org.example.integrations.PaymentProviderClient;
import org.example.publishers.PaymentEventsPublisher;
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

    @Autowired
    private OrdersClient  ordersClient;

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
                payment != null ? payment.getPaymentProvider() : null,
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
        if (payment.getPaymentProvider() == null) {
            logger.warn(PAY_VALIDATE, "Missing provider (orderId={}, userId={})",
                    payment.getOrderId(), payment.getUserId());
            throw new InvalidPaymentException("Provider is required");
        }
        if (payment.getPaymentProvider() != PaymentProvider.PAYPAL) {
            logger.warn(PAY_VALIDATE, "Unsupported provider={} (orderId={})", payment.getPaymentProvider(), payment.getOrderId());
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
    private PaymentEvent createEvent(Payment payment, PaymentEventType type, Integer statusCode) {
        PaymentEvent event = new PaymentEvent();
        event.setPayment(payment);
        event.setEventType(type);
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
    private void publishEvent(PaymentEventType type, Payment payment) {
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
                payment != null ? payment.getPaymentProvider() : null,
                payment != null ? payment.getAmount() : null
        );

        validatePaymentForProcess(payment);

        var existingOpt = paymentsRepository.findByOrderId(payment.getOrderId());

        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();

            logger.info(PAY_PROCESS, "Existing payment found for orderId={} (paymentId={}, status={}, providerRef={})",
                    existing.getOrderId(), existing.getId(), existing.getStatus(), existing.getProviderRef());

            if (existing.getStatus() == PaymentStatus.PENDING && existing.getProviderRef() != null) {
                return existing;
            }

            OrderDTO order = null;

            try {
                order = ordersClient.getOrder(existingOpt.get().getOrderId()).getBody();
            } catch (FeignException e) {
                String errorBody = e.contentUTF8();
                logger.error(PAY_PROVIDER, "(PaymentsService): Orders service error: {}", errorBody);
                throw new ExternalServiceException("(PaymentsService): Orders service error");
            }

            if (order == null) {
                if (order.getTotal() != payment.getAmount()) {
                    throw new InvalidPaymentException("Payment amount is different from order total amount");
                }
            }

            if (existing.getStatus() == PaymentStatus.CAPTURED ||
                    existing.getStatus() == PaymentStatus.REFUNDED ||
                    existing.getStatus() == PaymentStatus.CANCELED) {
                throw new InvalidPaymentException("This order already has a finalized payment: " + existing.getStatus());
            }

            existing.setUserId(payment.getUserId());
            existing.setAmount(payment.getAmount());
            existing.setPaymentProvider(payment.getPaymentProvider());
            existing.setStatus(PaymentStatus.PENDING);

            try {
                logger.info(PAY_PROVIDER, "Creating PayPal order for existing payment (paymentId={}, orderId={})",
                        existing.getId(), existing.getOrderId());

                paymentProviderClient.createPaymentOrder(existing); // vai setProviderRef(...)
                Payment updated = paymentsRepository.save(existing);

                createEvent(updated, PaymentEventType.PENDING, null);
                publishEvent(PaymentEventType.PENDING, updated);

                return updated;

            } catch (PaymentRefusedException e) {
                existing.setStatus(PaymentStatus.FAILED);
                Payment failed = paymentsRepository.save(existing);

                createEvent(failed, PaymentEventType.FAILED, 402);
                publishEvent(PaymentEventType.FAILED, failed);

                throw e;
            }
        }

        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentsRepository.save(payment);

        logger.info(PAY_PROCESS, "Payment saved as PENDING (paymentId={}, orderId={})",
                saved.getId(), saved.getOrderId());

        try {
            logger.info(PAY_PROVIDER, "Creating PayPal order (paymentId={}, orderId={})",
                    saved.getId(), saved.getOrderId());

            paymentProviderClient.createPaymentOrder(saved);
            Payment updated = paymentsRepository.save(saved);

            createEvent(updated, PaymentEventType.PENDING, null);
            publishEvent(PaymentEventType.PENDING, updated);

            logger.info(PAY_PROCESS, "Process payment completed (paymentId={}, status={}, providerRef={})",
                    updated.getId(), updated.getStatus(), updated.getProviderRef());

            return updated;

        } catch (PaymentRefusedException e) {
            saved.setStatus(PaymentStatus.FAILED);
            Payment failed = paymentsRepository.save(saved);

            createEvent(failed, PaymentEventType.FAILED, 402);
            publishEvent(PaymentEventType.FAILED, failed);

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

        if (payment.getPaymentProvider() != PaymentProvider.PAYPAL) {
            logger.warn(PAY_CAPTURE, "Payment is not PAYPAL (paymentId={}, provider={})",
                    payment.getId(), payment.getPaymentProvider());
            throw new InvalidPaymentException("Payment is not PAYPAL");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            logger.warn(PAY_CAPTURE, "Only PENDING can be captured (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());
            throw new InvalidPaymentException("Only PENDING PAYPAL payments can be captured");
        }

        logger.info(PAY_PROVIDER, "Capturing PayPal payment (paymentId={}, providerRef={})",
                payment.getId(), providerRef);

        paymentProviderClient.capturePayment(providerRef);

        payment.setStatus(PaymentStatus.CAPTURED);
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, PaymentEventType.CAPTURED, 200);
        publishEvent(PaymentEventType.CAPTURED, updated);

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

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            logger.warn(PAY_CANCEL, "Payment already canceled (paymentId={})", paymentId);
            throw new PaymentAlreadyCanceledException("Payment already canceled");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            logger.warn(PAY_CANCEL, "Cannot cancel refunded payment (paymentId={})", paymentId);
            throw new InvalidPaymentException("Cannot cancel a refunded payment");
        }

        payment.setStatus(PaymentStatus.CANCELED);
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, PaymentEventType.CANCEL, 200);
        publishEvent(PaymentEventType.CANCEL, updated);

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

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            logger.warn(PAY_REFUND, "Only CAPTURED payments can be refunded (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());
            throw new InvalidRefundException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, PaymentEventType.REFUND, 200);
        publishEvent(PaymentEventType.REFUND, updated);

        logger.info(PAY_REFUND, "Refund completed (paymentId={}, status={})",
                updated.getId(), updated.getStatus());

        return updated;
    }

    @Transactional
    public void onRefundApproved(UUID paymentId) {
        Payment payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentsRepository.save(payment);

        createEvent(updated, PaymentEventType.REFUND, 200);
        publishEvent(PaymentEventType.REFUND, updated);
    }

    @Transactional
    public void onOrderCreated(UUID orderId, UUID userId, float total) {
        logger.info(PAY_EVENT, "OrderCreated event received (orderId={}, userId={}, total={})",
                orderId, userId, total);

        if (paymentsRepository.findByOrderId(orderId).isPresent()) {
            logger.info(PAY_EVENT, "Ignoring duplicated OrderCreated (orderId={}) - payment already exists", orderId);
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(total);

        payment.setPaymentProvider(PaymentProvider.PAYPAL);

        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentsRepository.save(payment);

        createEvent(saved, PaymentEventType.PENDING, null);
        publishEvent(PaymentEventType.PENDING, saved);

        logger.info(PAY_EVENT, "Payment created from OrderCreated (paymentId={}, orderId={}, status={})",
                saved.getId(), saved.getOrderId(), saved.getStatus());
    }
}
