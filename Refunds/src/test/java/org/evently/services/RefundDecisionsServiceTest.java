package org.evently.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.evently.clients.UsersClient;
import org.evently.enums.DecisionType;
import org.evently.enums.RefundRequestStatus;
import org.evently.exceptions.ExternalServiceException;
import org.evently.exceptions.InvalidRefundRequestDecisionException;
import org.evently.exceptions.RefundRequestDecisionNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.publishers.RefundsEventsPublisher;
import org.evently.repositories.RefundDecisionsRepository;
import org.evently.repositories.RefundRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundDecisionsServiceTest {

    @Mock private RefundDecisionsRepository refundDecisionsRepository;
    @Mock private RefundRequestsRepository refundRequestsRepository;
    @Mock private UsersClient usersClient;
    @Mock private RefundsEventsPublisher refundsEventsPublisher;

    @InjectMocks private RefundDecisionsService refundDecisionsService;

    private UUID requestId;
    private UUID decidedBy;
    private RefundRequest pendingRequest;

    @BeforeEach
    void setup() {
        requestId = UUID.randomUUID();
        decidedBy = UUID.randomUUID();
        pendingRequest = new RefundRequest();
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(RefundRequestStatus.PENDING);
    }

    @Test
    void getRefundDecision_exists_returnsDecision() {
        UUID id = UUID.randomUUID();
        RefundDecision d = new RefundDecision();
        d.setId(id);

        when(refundDecisionsRepository.findById(id)).thenReturn(Optional.of(d));

        RefundDecision res = refundDecisionsService.getRefundDecision(id);
        assertEquals(id, res.getId());
    }

    @Test
    void getRefundDecision_notExists_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(refundDecisionsRepository.findById(id)).thenReturn(Optional.empty());

        RefundRequestDecisionNotFoundException ex = assertThrows(RefundRequestDecisionNotFoundException.class,
                () -> refundDecisionsService.getRefundDecision(id));

        assertEquals("Refund Decision not found", ex.getMessage());
    }

    @Test
    void getRefundDecisionByRequest_exists_returnsDecision() {
        RefundDecision d = new RefundDecision();
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);
        d.setRefundRequest(rr);

        when(refundDecisionsRepository.findByRefundRequest_Id(requestId)).thenReturn(Optional.of(d));

        RefundDecision res = refundDecisionsService.getRefundDecisionByRequest(requestId);
        assertEquals(requestId, res.getRefundRequest().getId());
    }

    @Test
    void getRefundDecisionByRequest_notExists_throwsNotFound() {
        when(refundDecisionsRepository.findByRefundRequest_Id(requestId)).thenReturn(Optional.empty());

        RefundRequestDecisionNotFoundException ex = assertThrows(RefundRequestDecisionNotFoundException.class,
                () -> refundDecisionsService.getRefundDecisionByRequest(requestId));

        assertEquals("Refund Decision not found", ex.getMessage());
    }

    @Test
    void registerRefundDecision_alreadyHasDecision_throwsInvalid() {
        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenReturn(null);
        when(refundRequestsRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(refundDecisionsRepository.existsByRefundRequest_Id(requestId)).thenReturn(true);

        InvalidRefundRequestDecisionException ex = assertThrows(InvalidRefundRequestDecisionException.class,
                () -> refundDecisionsService.registerRefundDecision(d));

        assertEquals("This refund request already has a decision", ex.getMessage());
        verify(refundDecisionsRepository, never()).save(any());
    }

    @Test
    void registerRefundDecision_requestNotPending_throwsInvalid() {
        RefundRequest approved = new RefundRequest();
        approved.setId(requestId);
        approved.setStatus(RefundRequestStatus.APPROVED);

        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenReturn(null);
        when(refundRequestsRepository.findById(requestId)).thenReturn(Optional.of(approved));
        when(refundDecisionsRepository.existsByRefundRequest_Id(requestId)).thenReturn(false);

        InvalidRefundRequestDecisionException ex = assertThrows(InvalidRefundRequestDecisionException.class,
                () -> refundDecisionsService.registerRefundDecision(d));

        assertEquals("Only PENDING refund requests can be decided", ex.getMessage());
        verify(refundDecisionsRepository, never()).save(any());
    }

    @Test
    void registerRefundDecision_userNotFound_throwsUserNotFound() {
        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenThrow(feignNotFound());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> refundDecisionsService.registerRefundDecision(d));

        assertTrue(ex.getMessage().contains("User not found"));
        verify(refundDecisionsRepository, never()).save(any());
    }

    @Test
    void registerRefundDecision_usersServiceError_throwsExternalServiceException() {
        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenThrow(feignGenericError(500));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> refundDecisionsService.registerRefundDecision(d));

        assertTrue(ex.getMessage().contains("Users service error"));
        verify(refundDecisionsRepository, never()).save(any());
    }

    @Test
    void registerRefundDecision_refundRequestNotFound_throwsRefundRequestNotFound() {
        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenReturn(null);
        when(refundRequestsRepository.findById(requestId)).thenReturn(Optional.empty());

        RefundRequestNotFoundException ex = assertThrows(RefundRequestNotFoundException.class,
                () -> refundDecisionsService.registerRefundDecision(d));

        assertEquals("Refund Request not found", ex.getMessage());
        verify(refundDecisionsRepository, never()).save(any());
    }

    @Test
    void registerRefundDecision_success_approve_setsApproved_andPublishesEvent() {
        RefundDecision d = baseDecision(DecisionType.APPROVE);
        when(usersClient.getUser(decidedBy)).thenReturn(null);
        when(refundRequestsRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(refundDecisionsRepository.existsByRefundRequest_Id(requestId)).thenReturn(false);

        when(refundDecisionsRepository.save(any(RefundDecision.class))).thenAnswer(inv -> {
            RefundDecision saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(refundRequestsRepository.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundDecision saved = refundDecisionsService.registerRefundDecision(d);

        assertNotNull(saved.getId());
        assertEquals(RefundRequestStatus.APPROVED, saved.getRefundRequest().getStatus());
        assertNotNull(saved.getRefundRequest().getDecisionAt());
        verify(refundsEventsPublisher).publishRefundRequestDecisionRegisteredEvent(saved);
    }

    @Test
    void registerRefundDecision_success_reject_setsRejected() {
        RefundDecision d = baseDecision(DecisionType.REJECT);
        when(usersClient.getUser(decidedBy)).thenReturn(null);
        when(refundRequestsRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(refundDecisionsRepository.existsByRefundRequest_Id(requestId)).thenReturn(false);

        when(refundDecisionsRepository.save(any(RefundDecision.class))).thenAnswer(inv -> {
            RefundDecision saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(refundRequestsRepository.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundDecision saved = refundDecisionsService.registerRefundDecision(d);

        assertEquals(RefundRequestStatus.REJECTED, saved.getRefundRequest().getStatus());
    }

    private RefundDecision baseDecision(DecisionType type) {
        RefundDecision d = new RefundDecision();
        d.setDecidedBy(decidedBy);
        d.setDecisionType(type);
        d.setDescription("x");
        RefundRequest rr = new RefundRequest();
        rr.setId(requestId);
        d.setRefundRequest(rr);
        return d;
    }

    private FeignException.NotFound feignNotFound() {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + decidedBy,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("not found", req, null, null);
    }

    private FeignException feignGenericError(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + decidedBy,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().status(status).reason("err").request(req).headers(Collections.emptyMap()).build();
        return FeignException.errorStatus("UsersClient#getUser", resp);
    }

}
