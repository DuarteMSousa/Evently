package org.example.controllers;

import org.example.dtos.PaymentCreatedResponseDTO;
import org.example.dtos.PaymentCreateDTO;
import org.example.dtos.PaymentDTO;
import org.example.dtos.PaymentStatusDTO;
import org.example.exceptions.*;
import org.example.models.Payment;
import org.example.services.PaymentsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
public class PaymentsController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentsController.class);

    private static final Marker PAYMENT_STATUS = MarkerFactory.getMarker("PAYMENT_STATUS");
    private static final Marker PAYMENT_USER_LIST = MarkerFactory.getMarker("PAYMENT_USER_LIST");
    private static final Marker PAYMENT_PROCESS = MarkerFactory.getMarker("PAYMENT_PROCESS");
    private static final Marker PAYPAL_CALLBACK = MarkerFactory.getMarker("PAYPAL_CALLBACK");
    private static final Marker PAYPAL_CANCEL = MarkerFactory.getMarker("PAYPAL_CANCEL");
    private static final Marker PAYMENT_CANCEL = MarkerFactory.getMarker("PAYMENT_CANCEL");
    private static final Marker PAYMENT_REFUND = MarkerFactory.getMarker("PAYMENT_REFUND");

    @Autowired
    private PaymentsService paymentsService;

    private final ModelMapper modelMapper;

    public PaymentsController() {
        this.modelMapper = new ModelMapper();
    }

    private PaymentDTO toPaymentDTO(Payment payment) {
        return modelMapper.map(payment, PaymentDTO.class);
    }

    @GetMapping("/check-payment-status/{paymentId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId) {
        /* HttpStatus(produces)
         * 200 OK - Payment status retrieved successfully.
         * 404 NOT_FOUND - Payment does not exist.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(PAYMENT_STATUS, "Method checkPaymentStatus entered for Payment ID: {}", paymentId);

        try {
            Payment payment = paymentsService.getPayment(paymentId);

            PaymentStatusDTO dto = new PaymentStatusDTO();
            dto.setPaymentId(payment.getId());
            dto.setStatus(payment.getStatus());

            logger.info(PAYMENT_STATUS, "200 OK returned, payment status retrieved (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());

            return ResponseEntity.ok(dto);

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_STATUS, "404 NOT_FOUND: Payment {} not found", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_STATUS, "400 BAD_REQUEST: Exception caught while checking payment status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user-payments/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - List of payments for the specified user retrieved successfully.
         * 404 NOT_FOUND - User does not exist.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(PAYMENT_USER_LIST, "Method getUserPayments entered for User ID: {}", userId);

        try {
            List<PaymentDTO> list = paymentsService.getPaymentsByUser(userId)
                    .stream()
                    .map(this::toPaymentDTO)
                    .collect(Collectors.toList());

            logger.info(PAYMENT_USER_LIST, "200 OK returned, user payments retrieved (userId={}, results={})",
                    userId, list.size());

            return ResponseEntity.ok(list);

        } catch (UserNotFoundException e) {
            logger.warn(PAYMENT_USER_LIST, "404 NOT_FOUND: User {} not found", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_USER_LIST, "400 BAD_REQUEST: Exception caught while getting user payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/process-payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentCreateDTO dto) {
        /* HttpStatus(produces)
         * 201 CREATED - Payment created and initiated successfully.
         * 402 PAYMENT_REQUIRED - Payment refused by provider.
         * 400 BAD_REQUEST - Invalid data provided / generic error.
         */

        logger.info(PAYMENT_PROCESS,
                "Method processPayment entered (orderId={}, userId={}, amount={}, provider={})",
                dto.getOrderId(), dto.getUserId(), dto.getAmount(), dto.getProvider());

        try {
            Payment paymentRequest = new Payment();
            paymentRequest.setOrderId(dto.getOrderId());
            paymentRequest.setUserId(dto.getUserId());
            paymentRequest.setAmount(dto.getAmount());
            paymentRequest.setPaymentProvider(dto.getProvider());

            Payment created = paymentsService.processPayment(paymentRequest);

            String approvalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=" + created.getProviderRef();

            PaymentCreatedResponseDTO response = new PaymentCreatedResponseDTO();
            response.setPaymentId(created.getId());
            response.setStatus(created.getStatus());
            response.setProvider(created.getPaymentProvider());
            response.setProviderRef(created.getProviderRef());
            response.setApprovalUrl(approvalUrl);

            logger.info(PAYMENT_PROCESS,
                    "201 CREATED returned, payment processed (paymentId={}, status={}, provider={}, providerRef={})",
                    created.getId(), created.getStatus(), created.getPaymentProvider(), created.getProviderRef());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidPaymentException e) {
            logger.warn(PAYMENT_PROCESS,
                    "400 BAD_REQUEST: Invalid payment payload (orderId={}, userId={}) reason={}",
                    dto.getOrderId(), dto.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (PaymentRefusedException e) {
            logger.warn(PAYMENT_PROCESS,
                    "402 PAYMENT_REQUIRED: Payment refused by provider (orderId={}, userId={}, provider={}) reason={}",
                    dto.getOrderId(), dto.getUserId(), dto.getProvider(), e.getMessage());
            return ResponseEntity.status(402).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_PROCESS,
                    "400 BAD_REQUEST: Exception caught while processing payment: {}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/paypal-callback")
    public ResponseEntity<?> paypalCallback(@RequestParam("token") String token) {
        /* HttpStatus(produces)
         * 200 OK - Payment updated successfully after PayPal callback/capture.
         * 404 NOT_FOUND - Payment does not exist.
         * 400 BAD_REQUEST - Invalid capture / generic error.
         */

        logger.info(PAYPAL_CALLBACK, "Method paypalCallback entered (token={})", token);

        try {
            Payment updated = paymentsService.capturePaypalPayment(token);
            PaymentDTO dto = toPaymentDTO(updated);

            logger.info(PAYPAL_CALLBACK,
                    "200 OK returned, PayPal capture succeeded (paymentId={}, status={}, providerRef={})",
                    updated.getId(), updated.getStatus(), updated.getProviderRef());

            return ResponseEntity.ok(dto);

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYPAL_CALLBACK, "404 NOT_FOUND: Payment not found for token={}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidPaymentException | PaymentRefusedException e) {
            logger.warn(PAYPAL_CALLBACK, "400 BAD_REQUEST: Invalid/refused PayPal capture (token={}) reason={}",
                    token, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYPAL_CALLBACK, "400 BAD_REQUEST: Exception caught while handling PayPal callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/paypal-cancel")
    public ResponseEntity<?> paypalCancel(@RequestParam(value = "token", required = false) String token) {
        /* HttpStatus(produces)
         * 200 OK - Payment cancelled by user on PayPal.
         */

        logger.info(PAYPAL_CANCEL, "Method paypalCancel entered (token={})", token);

        // Podes só devolver uma mensagem ou no futuro marcar como FAILED/CANCELED
        logger.info(PAYPAL_CANCEL, "200 OK returned, PayPal cancellation acknowledged (token={})", token);
        return ResponseEntity.ok("Pagamento cancelado pelo utilizador no PayPal.");
    }

    @PostMapping("/cancel-payment/{paymentId}")
    public ResponseEntity<?> cancelPayment(@PathVariable("paymentId") UUID paymentId) {
        /* HttpStatus(produces)
         * 200 OK - Payment cancelled successfully.
         * 404 NOT_FOUND - Payment does not exist.
         * 400 BAD_REQUEST - Invalid state (already cancelled) / invalid request / generic error.
         */

        logger.info(PAYMENT_CANCEL, "Method cancelPayment entered for Payment ID: {}", paymentId);

        try {
            Payment updated = paymentsService.cancelPayment(paymentId);

            logger.info(PAYMENT_CANCEL, "200 OK returned, payment cancelled (paymentId={}, status={})",
                    updated.getId(), updated.getStatus());

            return ResponseEntity.ok(toPaymentDTO(updated));

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_CANCEL, "404 NOT_FOUND: Payment {} not found for cancellation", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (PaymentAlreadyCanceledException | InvalidPaymentException e) {
            logger.warn(PAYMENT_CANCEL, "400 BAD_REQUEST: Invalid cancellation state (paymentId={}) reason={}",
                    paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_CANCEL, "400 BAD_REQUEST: Exception caught while cancelling payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/process-refund/{paymentId}")
    public ResponseEntity<?> processRefund(@PathVariable("paymentId") UUID paymentId) {
        /* HttpStatus(produces)
         * 201 CREATED - Refund processed successfully.
         * 404 NOT_FOUND - Payment does not exist.
         * 400 BAD_REQUEST - Invalid refund / generic error.
         */

        logger.info(PAYMENT_REFUND, "Method processRefund entered for Payment ID: {}", paymentId);

        try {
            Payment refunded = paymentsService.processRefund(paymentId);

            logger.info(PAYMENT_REFUND, "201 CREATED returned, refund processed (paymentId={}, status={})",
                    refunded.getId(), refunded.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(toPaymentDTO(refunded));

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_REFUND, "404 NOT_FOUND: Payment {} not found for refund", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidRefundException e) {
            logger.warn(PAYMENT_REFUND, "400 BAD_REQUEST: Invalid refund (paymentId={}) reason={}",
                    paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_REFUND, "400 BAD_REQUEST: Exception caught while processing refund: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
