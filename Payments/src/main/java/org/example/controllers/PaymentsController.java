package org.example.controllers;


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

    // ---------- GET /check-payment-status/{paymentId} ----------

    @GetMapping("/check-payment-status/{paymentId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable("paymentId") UUID paymentId) {
        /*
         * 200 – Estado encontrado
         * 404 – Pagamento não encontrado
         */
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

    // ---------- GET /user-payments/{userId} ----------

    @GetMapping("/user-payments/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable("userId") UUID userId) {
        /*
         * 200 – Pagamentos encontrados
         * 404 – Utilizador não encontrado
         */
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

    // ---------- POST /process-payment ----------

    @PostMapping("/process-payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentCreateDTO dto) {
        /*
         * 201 – Pagamento criado
         * 400 – Campos inválidos
         * 402 – Pagamento recusado
         */
        try {
            Payment payment = modelMapper.map(dto, Payment.class);

            Payment created = paymentsService.processPayment(payment);

            return ResponseEntity.status(HttpStatus.CREATED).body(toPaymentDTO(created));
        } catch (InvalidPaymentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (PaymentRefusedException e) {
            // 402 Payment Required
            return ResponseEntity.status(402).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ---------- POST /cancel-payment/{paymentId} ----------

    @PostMapping("/cancel-payment/{paymentId}")
    public ResponseEntity<?> cancelPayment(@PathVariable("paymentId") UUID paymentId) {
        /*
         * 200 – Pagamento cancelado
         * 404 – Pagamento não encontrado
         */
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

    // ---------- POST /process-refund/{paymentId} ----------

    @PostMapping("/process-refund/{paymentId}")
    public ResponseEntity<?> processRefund(@PathVariable("paymentId") UUID paymentId) {
        /*
         * 201 – Reembolso processado
         * 400 – Campos inválidos
         * 404 – Pagamento não encontrado
         */
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