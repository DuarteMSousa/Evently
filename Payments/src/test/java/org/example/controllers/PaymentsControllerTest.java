package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dtos.PaymentCreateDTO;
import org.example.enums.PaymentProvider;
import org.example.enums.PaymentStatus;
import org.example.exceptions.*;
import org.example.models.Payment;
import org.example.services.PaymentsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PaymentsService paymentsService;

    private Payment basePayment() {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setOrderId(UUID.randomUUID());
        p.setUserId(UUID.randomUUID());
        p.setAmount(10f);
        p.setPaymentProvider(PaymentProvider.PAYPAL);
        p.setStatus(PaymentStatus.PENDING);
        p.setProviderRef("TOK");
        return p;
    }

    // GET /check-payment-status/{id}

    @Test
    void checkPaymentStatus_200() throws Exception {
        Payment p = basePayment();
        when(paymentsService.getPayment(p.getId())).thenReturn(p);

        mockMvc.perform(get("/payments/check-payment-status/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(p.getId().toString()))
                .andExpect(jsonPath("$.status").value(p.getStatus().name()));
    }

    @Test
    void checkPaymentStatus_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.getPayment(id)).thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(get("/payments/check-payment-status/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Payment not found"));
    }

    @Test
    void checkPaymentStatus_400_generic() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.getPayment(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/payments/check-payment-status/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    // GET /user-payments/{userId}

    @Test
    void getUserPayments_200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(paymentsService.getPaymentsByUser(userId)).thenReturn(Arrays.asList(basePayment(), basePayment()));

        mockMvc.perform(get("/payments/user-payments/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getUserPayments_404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(paymentsService.getPaymentsByUser(userId)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/payments/user-payments/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    // POST /process-payment

    @Test
    void processPayment_201() throws Exception {
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setOrderId(UUID.randomUUID());
        dto.setUserId(UUID.randomUUID());
        dto.setAmount(10f);
        dto.setProvider(PaymentProvider.PAYPAL);

        Payment created = basePayment();
        created.setOrderId(dto.getOrderId());
        created.setUserId(dto.getUserId());
        created.setAmount(dto.getAmount());
        created.setPaymentProvider(dto.getProvider());
        created.setStatus(PaymentStatus.PENDING);
        created.setProviderRef("TOK-1");

        when(paymentsService.processPayment(any(Payment.class))).thenReturn(created);

        mockMvc.perform(post("/payments/process-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(created.getId().toString()))
                .andExpect(jsonPath("$.status").value(created.getStatus().name()))
                .andExpect(jsonPath("$.provider").value(created.getPaymentProvider().name()))
                .andExpect(jsonPath("$.providerRef").value(created.getProviderRef()))
                .andExpect(jsonPath("$.approvalUrl").exists());
    }

    @Test
    void processPayment_400_invalidPayment() throws Exception {
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setOrderId(UUID.randomUUID());
        dto.setUserId(UUID.randomUUID());
        dto.setAmount(0f);
        dto.setProvider(PaymentProvider.PAYPAL);

        when(paymentsService.processPayment(any(Payment.class)))
                .thenThrow(new InvalidPaymentException("Amount must be greater than 0"));

        mockMvc.perform(post("/payments/process-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount must be greater than 0"));
    }

    @Test
    void processPayment_402_refused() throws Exception {
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setOrderId(UUID.randomUUID());
        dto.setUserId(UUID.randomUUID());
        dto.setAmount(10f);
        dto.setProvider(PaymentProvider.PAYPAL);

        when(paymentsService.processPayment(any(Payment.class)))
                .thenThrow(new PaymentRefusedException("refused"));

        mockMvc.perform(post("/payments/process-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().string("refused"));
    }

    // GET /paypal-callback?token=

    @Test
    void paypalCallback_200() throws Exception {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CAPTURED);

        when(paymentsService.capturePaypalPayment("TOK")).thenReturn(p);

        mockMvc.perform(get("/payments/paypal-callback").param("token", "TOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.status").value(p.getStatus().name()));
    }

    @Test
    void paypalCallback_404() throws Exception {
        when(paymentsService.capturePaypalPayment("TOK"))
                .thenThrow(new PaymentNotFoundException("not found"));

        mockMvc.perform(get("/payments/paypal-callback").param("token", "TOK"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("not found"));
    }

    @Test
    void paypalCallback_400_invalidOrRefused() throws Exception {
        when(paymentsService.capturePaypalPayment("TOK"))
                .thenThrow(new InvalidPaymentException("invalid"));

        mockMvc.perform(get("/payments/paypal-callback").param("token", "TOK"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid"));
    }

    // GET /paypal-cancel

    @Test
    void paypalCancel_200() throws Exception {
        mockMvc.perform(get("/payments/paypal-cancel").param("token", "TOK"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pagamento cancelado pelo utilizador no PayPal."));
    }

    // POST /cancel-payment/{paymentId}

    @Test
    void cancelPayment_200() throws Exception {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CANCELED);

        when(paymentsService.cancelPayment(p.getId())).thenReturn(p);

        mockMvc.perform(post("/payments/cancel-payment/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.CANCELED.name()));
    }

    @Test
    void cancelPayment_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.cancelPayment(id)).thenThrow(new PaymentNotFoundException("not found"));

        mockMvc.perform(post("/payments/cancel-payment/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("not found"));
    }

    @Test
    void cancelPayment_400_invalidState() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.cancelPayment(id)).thenThrow(new PaymentAlreadyCanceledException("already"));

        mockMvc.perform(post("/payments/cancel-payment/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("already"));
    }

    // POST /process-refund/{paymentId}

    @Test
    void processRefund_201() throws Exception {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.REFUNDED);

        when(paymentsService.processRefund(p.getId())).thenReturn(p);

        mockMvc.perform(post("/payments/process-refund/{id}", p.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.REFUNDED.name()));
    }

    @Test
    void processRefund_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.processRefund(id)).thenThrow(new PaymentNotFoundException("not found"));

        mockMvc.perform(post("/payments/process-refund/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("not found"));
    }

    @Test
    void processRefund_400() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentsService.processRefund(id)).thenThrow(new InvalidRefundException("invalid"));

        mockMvc.perform(post("/payments/process-refund/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid"));
    }

}
