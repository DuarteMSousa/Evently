package org.evently.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.evently.clients.PaymentsClient;
import org.evently.clients.UsersClient;
import org.evently.dtos.externalServices.PaymentStatusDTO;
import org.evently.enums.RefundRequestStatus;
import org.evently.enums.externalServices.PaymentStatus;
import org.evently.exceptions.ExternalServiceException;
import org.evently.exceptions.InvalidRefundRequestException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.exceptions.externalServices.PaymentNotFoundException;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundRequestsServiceTest {

    @Mock private RefundRequestsRepository refundRequestsRepository;
    @Mock private UsersClient usersClient;
    @Mock private PaymentsClient paymentsClient;

    @InjectMocks private RefundRequestsService refundRequestsService;

    private RefundRequest valid;
    private UUID paymentId;
    private UUID userId;

    @BeforeEach
    void setup() {
        paymentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        valid = new RefundRequest();
        valid.setPaymentId(paymentId);
        valid.setUserId(userId);
        valid.setTitle("t");
        valid.setDescription("d");
    }

    @Test
    void getRefundRequest_exists_returns() {
        UUID id = UUID.randomUUID();
        RefundRequest rr = new RefundRequest(); rr.setId(id);
        when(refundRequestsRepository.findById(id)).thenReturn(Optional.of(rr));

        RefundRequest res = refundRequestsService.getRefundRequest(id);
        assertEquals(id, res.getId());
    }

    @Test
    void getRefundRequest_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(refundRequestsRepository.findById(id)).thenReturn(Optional.empty());

        RefundRequestNotFoundException ex = assertThrows(RefundRequestNotFoundException.class,
                () -> refundRequestsService.getRefundRequest(id));

        assertEquals("Refund Request not found", ex.getMessage());
    }

    @Test
    void createRefundRequest_paymentIdNull_throwsInvalid() {
        valid.setPaymentId(null);

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("Payment ID is required", ex.getMessage());
        verifyNoInteractions(usersClient, paymentsClient);
    }

    @Test
    void createRefundRequest_userIdNull_throwsInvalid() {
        valid.setUserId(null);

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("User ID is required", ex.getMessage());
        verifyNoInteractions(usersClient, paymentsClient);
    }

    @Test
    void createRefundRequest_titleBlank_throwsInvalid() {
        valid.setTitle("   ");

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("Title is required", ex.getMessage());
        verifyNoInteractions(usersClient, paymentsClient);
    }

    @Test
    void createRefundRequest_descriptionBlank_throwsInvalid() {
        valid.setDescription("");

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("Description is required", ex.getMessage());
        verifyNoInteractions(usersClient, paymentsClient);
    }

    @Test
    void createRefundRequest_activeRefundExists_throwsInvalid() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(true);

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("There is already an active or processed refund for this payment", ex.getMessage());
        verifyNoInteractions(usersClient, paymentsClient);
    }

    @Test
    void createRefundRequest_userNotFound_throwsUserNotFound() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenThrow(feignNotFound());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertTrue(ex.getMessage().contains("User not found"));
        verifyNoInteractions(paymentsClient);
    }

    @Test
    void createRefundRequest_usersServiceError_throwsExternal() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenThrow(feignGenericError(503));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertTrue(ex.getMessage().contains("Users service error"));
        verifyNoInteractions(paymentsClient);
    }

    @Test
    void createRefundRequest_paymentNotFound_throwsPaymentNotFound() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenReturn(null);
        when(paymentsClient.checkPaymentStatus(paymentId)).thenThrow(feignNotFound());

        PaymentNotFoundException ex = assertThrows(PaymentNotFoundException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertTrue(ex.getMessage().contains("Payment not found"));
        verify(refundRequestsRepository, never()).save(any());
    }

    @Test
    void createRefundRequest_paymentsServiceError_throwsExternal() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenReturn(null);
        when(paymentsClient.checkPaymentStatus(paymentId)).thenThrow(feignGenericError(500));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertTrue(ex.getMessage().contains("Payments service error"));
        verify(refundRequestsRepository, never()).save(any());
    }

    @Test
    void createRefundRequest_paymentNotCaptured_throwsInvalid() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenReturn(null);

        when(paymentsClient.checkPaymentStatus(paymentId)).thenReturn(mockPaymentStatus(PaymentStatus.PENDING));

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.createRefundRequest(valid));

        assertEquals("Payment is not processed yet", ex.getMessage());
        verify(refundRequestsRepository, never()).save(any());
    }

    @Test
    void createRefundRequest_success_savesWithPending() {
        when(refundRequestsRepository.existsByPaymentIdAndStatusIn(eq(paymentId), any())).thenReturn(false);
        when(usersClient.getUser(userId)).thenReturn(null);

        when(paymentsClient.checkPaymentStatus(paymentId)).thenReturn(mockPaymentStatus(PaymentStatus.CAPTURED));

        when(refundRequestsRepository.save(any(RefundRequest.class))).thenAnswer(inv -> {
            RefundRequest rr = inv.getArgument(0);
            rr.setId(UUID.randomUUID());
            return rr;
        });

        RefundRequest saved = refundRequestsService.createRefundRequest(valid);

        assertNotNull(saved.getId());
        assertEquals(RefundRequestStatus.PENDING, saved.getStatus());
    }

    @Test
    void markAsProcessed_notApproved_throwsInvalid() {
        UUID id = UUID.randomUUID();
        RefundRequest rr = new RefundRequest();
        rr.setId(id);
        rr.setStatus(RefundRequestStatus.PENDING);

        when(refundRequestsRepository.findById(id)).thenReturn(Optional.of(rr));

        InvalidRefundRequestException ex = assertThrows(InvalidRefundRequestException.class,
                () -> refundRequestsService.markAsProcessed(id));

        assertTrue(ex.getMessage().startsWith("Cannot process refund in status:"));
        verify(refundRequestsRepository, never()).save(any());
    }

    @Test
    void markAsProcessed_success_setsProcessedAndDate() {
        UUID id = UUID.randomUUID();
        RefundRequest rr = new RefundRequest();
        rr.setId(id);
        rr.setStatus(RefundRequestStatus.APPROVED);

        when(refundRequestsRepository.findById(id)).thenReturn(Optional.of(rr));
        when(refundRequestsRepository.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequest updated = refundRequestsService.markAsProcessed(id);

        assertEquals(RefundRequestStatus.PROCESSED, updated.getStatus());
        assertNotNull(updated.getProcessedAt());
    }

    @Test
    void getActiveRefundRequestByPayment_notFound_throws() {
        when(refundRequestsRepository.findOneByPaymentIdAndStatus(eq(paymentId), eq(RefundRequestStatus.APPROVED)))
                .thenReturn(null);

        RefundRequestNotFoundException ex = assertThrows(RefundRequestNotFoundException.class,
                () -> refundRequestsService.getActiveRefundRequestByPayment(paymentId));

        assertEquals("No active refund request found for this payment", ex.getMessage());
    }

    @Test
    void getActiveRefundRequestByPayment_found_returns() {
        RefundRequest rr = new RefundRequest();
        rr.setId(UUID.randomUUID());
        rr.setPaymentId(paymentId);
        rr.setStatus(RefundRequestStatus.APPROVED);

        when(refundRequestsRepository.findOneByPaymentIdAndStatus(eq(paymentId), eq(RefundRequestStatus.APPROVED)))
                .thenReturn(rr);

        RefundRequest res = refundRequestsService.getActiveRefundRequestByPayment(paymentId);
        assertEquals(rr.getId(), res.getId());
    }

    // helpers
    private ResponseEntity<PaymentStatusDTO> mockPaymentStatus(PaymentStatus status) {
        PaymentStatusDTO dto = new PaymentStatusDTO();
        dto.setStatus(status);
        return ResponseEntity.ok(dto);
    }

    private FeignException.NotFound feignNotFound() {
        Request req = Request.create(Request.HttpMethod.GET, "/x",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("not found", req, null, null);
    }

    private FeignException feignGenericError(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/x",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().status(status).reason("err").request(req).headers(Collections.emptyMap()).build();
        return FeignException.errorStatus("Client#call", resp);
    }
}
