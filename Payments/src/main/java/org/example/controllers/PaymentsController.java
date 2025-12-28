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

    /*
     * 200 OK - Estado do pagamento encontrado
     * 404 NOT_FOUND - Pagamento não encontrado
     * 400 BAD_REQUEST - Erro genérico
     */
    @GetMapping("/check-payment-status/{paymentId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId) {
        logger.info(PAYMENT_STATUS, "GET /payments/check-payment-status/{} requested", paymentId);

        try {
            Payment payment = paymentsService.getPayment(paymentId);

            PaymentStatusDTO dto = new PaymentStatusDTO();
            dto.setPaymentId(payment.getId());
            dto.setStatus(payment.getStatus());

            logger.info(PAYMENT_STATUS, "Check payment status succeeded (paymentId={}, status={})",
                    payment.getId(), payment.getStatus());

            return ResponseEntity.status(HttpStatus.OK).body(dto);

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_STATUS, "Check payment status failed - not found (paymentId={})", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_STATUS, "Check payment status failed - unexpected error (paymentId={})",
                    paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * 200 OK - Lista de pagamentos do utilizador encontrada
     * 404 NOT_FOUND - Utilizador não encontrado
     * 400 BAD_REQUEST - Erro genérico
     */
    @GetMapping("/user-payments/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable("userId") UUID userId) {
        logger.info(PAYMENT_USER_LIST, "GET /payments/user-payments/{} requested", userId);

        try {
            List<PaymentDTO> list = paymentsService.getPaymentsByUser(userId)
                    .stream()
                    .map(this::toPaymentDTO)
                    .collect(Collectors.toList());

            logger.info(PAYMENT_USER_LIST, "Get user payments succeeded (userId={}, results={})",
                    userId, list.size());

            return ResponseEntity.status(HttpStatus.OK).body(list);

        } catch (UserNotFoundException e) {
            logger.warn(PAYMENT_USER_LIST, "Get user payments failed - user not found (userId={})", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_USER_LIST, "Get user payments failed - unexpected error (userId={})",
                    userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * 201 CREATED - Pagamento criado e iniciado
     * 400 BAD_REQUEST - Campos inválidos / Erro genérico
     * 402 PAYMENT_REQUIRED - Pagamento recusado pelo provedor
     */
    @PostMapping("/process-payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentCreateDTO dto) {
        logger.info(PAYMENT_PROCESS,
                "POST /payments/process-payment requested (orderId={}, userId={}, amount={}, provider={})",
                dto.getOrderId(), dto.getUserId(), dto.getAmount(), dto.getProvider());

        try {
            Payment payment = new Payment();
            payment.setOrderId(dto.getOrderId());
            payment.setUserId(dto.getUserId());
            payment.setAmount(dto.getAmount());
            payment.setProvider(dto.getProvider());

            Payment created = paymentsService.processPayment(payment);

            String approvalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=" + created.getProviderRef();

            PaymentCreatedResponseDTO response = new PaymentCreatedResponseDTO();
            response.setPaymentId(created.getId());
            response.setStatus(created.getStatus());
            response.setProvider(created.getProvider());
            response.setProviderRef(created.getProviderRef());
            response.setApprovalUrl(approvalUrl);

            logger.info(PAYMENT_PROCESS,
                    "Process payment succeeded (paymentId={}, status={}, provider={}, providerRef={})",
                    created.getId(), created.getStatus(), created.getProvider(), created.getProviderRef());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidPaymentException e) {
            logger.warn(PAYMENT_PROCESS,
                    "Process payment failed - invalid payload (orderId={}, userId={}) reason={}",
                    dto.getOrderId(), dto.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (PaymentRefusedException e) {
            logger.warn(PAYMENT_PROCESS,
                    "Process payment failed - refused by provider (orderId={}, userId={}, provider={}) reason={}",
                    dto.getOrderId(), dto.getUserId(), dto.getProvider(), e.getMessage());
            // 402 Payment Required
            return ResponseEntity.status(402).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_PROCESS,
                    "Process payment failed - unexpected error (orderId={}, userId={}, provider={})",
                    dto.getOrderId(), dto.getUserId(), dto.getProvider(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * 200 OK - Pagamento atualizado após callback do PayPal
     * 404 NOT_FOUND - Pagamento não encontrado
     * 400 BAD_REQUEST - Erro genérico / Captura inválida
     */
    @GetMapping("/paypal-callback")
    public ResponseEntity<?> paypalCallback(@RequestParam("token") String token) {
        logger.info(PAYPAL_CALLBACK, "GET /payments/paypal-callback requested (token={})", token);

        try {
            Payment updated = paymentsService.capturePaypalPayment(token);
            PaymentDTO dto = toPaymentDTO(updated);

            logger.info(PAYPAL_CALLBACK,
                    "PayPal callback capture succeeded (paymentId={}, status={}, providerRef={})",
                    updated.getId(), updated.getStatus(), updated.getProviderRef());

            return ResponseEntity.status(HttpStatus.OK).body(dto);

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYPAL_CALLBACK, "PayPal callback failed - payment not found (token={})", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidPaymentException | PaymentRefusedException e) {
            logger.warn(PAYPAL_CALLBACK, "PayPal callback failed - invalid/refused capture (token={}) reason={}",
                    token, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYPAL_CALLBACK, "PayPal callback failed - unexpected error (token={})", token, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * 200 OK - Pagamento cancelado pelo utilizador
     */
    @GetMapping("/paypal-cancel")
    public ResponseEntity<?> paypalCancel(@RequestParam(value = "token", required = false) String token) {
        logger.info(PAYPAL_CANCEL, "GET /payments/paypal-cancel requested (token={})", token);

        // Podes só devolver uma mensagem ou no futuro marcar como FAILED/CANCELED
        return ResponseEntity.status(HttpStatus.OK).body("Pagamento cancelado pelo utilizador no PayPal.");
    }

    /*
     * 200 OK - Pagamento cancelado
     * 404 NOT_FOUND - Pagamento não encontrado
     * 400 BAD_REQUEST - Erro genérico / Já cancelado / Estado inválido
     */
    @PostMapping("/cancel-payment/{paymentId}")
    public ResponseEntity<?> cancelPayment(@PathVariable("paymentId") UUID paymentId) {
        logger.info(PAYMENT_CANCEL, "POST /payments/cancel-payment/{} requested", paymentId);

        try {
            Payment updated = paymentsService.cancelPayment(paymentId);

            logger.info(PAYMENT_CANCEL, "Cancel payment succeeded (paymentId={}, status={})",
                    updated.getId(), updated.getStatus());

            return ResponseEntity.status(HttpStatus.OK).body(toPaymentDTO(updated));

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_CANCEL, "Cancel payment failed - not found (paymentId={})", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (PaymentAlreadyCanceledException | InvalidPaymentException e) {
            logger.warn(PAYMENT_CANCEL, "Cancel payment failed - invalid state (paymentId={}) reason={}",
                    paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_CANCEL, "Cancel payment failed - unexpected error (paymentId={})",
                    paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /*
     * 201 CREATED - Reembolso processado
     * 404 NOT_FOUND - Pagamento não encontrado
     * 400 BAD_REQUEST - Erro genérico / Reembolso inválido
     */
    @PostMapping("/process-refund/{paymentId}")
    public ResponseEntity<?> processRefund(@PathVariable("paymentId") UUID paymentId) {
        logger.info(PAYMENT_REFUND, "POST /payments/process-refund/{} requested", paymentId);

        try {
            Payment refunded = paymentsService.processRefund(paymentId);

            logger.info(PAYMENT_REFUND, "Process refund succeeded (paymentId={}, status={})",
                    refunded.getId(), refunded.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(toPaymentDTO(refunded));

        } catch (PaymentNotFoundException e) {
            logger.warn(PAYMENT_REFUND, "Process refund failed - payment not found (paymentId={})", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidRefundException e) {
            logger.warn(PAYMENT_REFUND, "Process refund failed - invalid refund (paymentId={}) reason={}",
                    paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(PAYMENT_REFUND, "Process refund failed - unexpected error (paymentId={})",
                    paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
