package org.example.controllers;

import org.example.dtos.PaymentCreatedResponseDTO;
import org.example.dtos.PaymentCreateDTO;
import org.example.dtos.PaymentDTO;
import org.example.dtos.PaymentStatusDTO;
import org.example.exceptions.*;
import org.example.models.Payment;
import org.example.services.PaymentsService;
import org.modelmapper.ModelMapper;
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

    @Autowired
    private PaymentsService paymentsService;

    private final ModelMapper modelMapper;

    public PaymentsController() {
        this.modelMapper = new ModelMapper();
    }

    private PaymentDTO toPaymentDTO(Payment payment) {
        return modelMapper.map(payment, PaymentDTO.class);
    }

    // -------------------------------------------------------
    // GET /check-payment-status/{paymentId}
    // -------------------------------------------------------
    @GetMapping("/check-payment-status/{paymentId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId) {

        try {
            Payment payment = paymentsService.getPayment(paymentId);

            PaymentStatusDTO dto = new PaymentStatusDTO();
            dto.setPaymentId(payment.getId());
            dto.setStatus(payment.getStatus());

            return ResponseEntity.status(HttpStatus.OK).body(dto);

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // GET /user-payments/{userId}
    // -------------------------------------------------------
    @GetMapping("/user-payments/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable("userId") UUID userId) {

        try {
            List<PaymentDTO> list = paymentsService.getPaymentsByUser(userId)
                    .stream()
                    .map(this::toPaymentDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(list);

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // POST /process-payment  (cria ordem PayPal, devolve approvalUrl)
    // -------------------------------------------------------
    @PostMapping("/process-payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentCreateDTO dto) {

        try {
            // DTO -> Entity (manual para não chatear o ModelMapper com ids)
            Payment payment = new Payment();
            payment.setOrderId(dto.getOrderId());
            payment.setUserId(dto.getUserId());
            payment.setAmount(dto.getAmount());
            payment.setProvider(dto.getProvider());

            Payment created = paymentsService.processPayment(payment);

            // Construímos o approvalUrl a partir do providerRef (PayPal order id)
            // Formato típico do PayPal Checkout:
            String approvalUrl = "https://www.sandbox.paypal.com/checkoutnow?token="
                    + created.getProviderRef();

            PaymentCreatedResponseDTO response = new PaymentCreatedResponseDTO();
            response.setPaymentId(created.getId());
            response.setStatus(created.getStatus());
            response.setProvider(created.getProvider());
            response.setProviderRef(created.getProviderRef());
            response.setApprovalUrl(approvalUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidPaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (PaymentRefusedException e) {
            // 402 Payment Required
            return ResponseEntity.status(402).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // GET /paypal-callback?token=...
    // Este endpoint simula o "return_url" do PayPal
    // -------------------------------------------------------
    @GetMapping("/paypal-callback")
    public ResponseEntity<?> paypalCallback(@RequestParam("token") String token) {

        try {
            // token == PayPal order id == providerRef
            Payment updated = paymentsService.capturePaypalPayment(token);
            PaymentDTO dto = toPaymentDTO(updated);

            return ResponseEntity.status(HttpStatus.OK).body(dto);

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidPaymentException | PaymentRefusedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // (Opcional) cancelar via return_url de cancel do PayPal
    @GetMapping("/paypal-cancel")
    public ResponseEntity<?> paypalCancel(@RequestParam(value = "token", required = false) String token) {
        // Podes só devolver uma mensagem ou no futuro marcar como FAILED/CANCELED
        return ResponseEntity.status(HttpStatus.OK).body("Pagamento cancelado pelo utilizador no PayPal.");
    }

    // -------------------------------------------------------
    // POST /cancel-payment/{paymentId}
    // -------------------------------------------------------
    @PostMapping("/cancel-payment/{paymentId}")
    public ResponseEntity<?> cancelPayment(@PathVariable("paymentId") UUID paymentId) {

        try {
            Payment updated = paymentsService.cancelPayment(paymentId);
            return ResponseEntity.status(HttpStatus.OK).body(toPaymentDTO(updated));

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (PaymentAlreadyCanceledException | InvalidPaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // POST /process-refund/{paymentId}
    // -------------------------------------------------------
    @PostMapping("/process-refund/{paymentId}")
    public ResponseEntity<?> processRefund(@PathVariable("paymentId") UUID paymentId) {

        try {
            Payment refunded = paymentsService.processRefund(paymentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toPaymentDTO(refunded));

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidRefundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}