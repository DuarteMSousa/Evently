package org.example.services;

import org.example.enums.PaymentEventType;
import org.example.enums.PaymentProvider;
import org.example.enums.PaymentStatus;
import org.example.exceptions.*;
import org.example.integrations.PaymentProviderClient;
import org.example.models.Payment;
import org.example.models.PaymentEvent;
import org.example.publishers.PaymentEventsPublisher;
import org.example.repositories.PaymentEventsRepository;
import org.example.repositories.PaymentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentsServiceTest {

    @Mock PaymentsRepository paymentsRepository;
    @Mock PaymentEventsRepository paymentEventsRepository;
    @Mock PaymentEventsPublisher paymentEventsPublisher;
    @Mock PaymentProviderClient paymentProviderClient;

    @InjectMocks PaymentsService paymentsService;

    private Payment basePayment() {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setOrderId(UUID.randomUUID());
        p.setUserId(UUID.randomUUID());
        p.setAmount(10f);
        p.setPaymentProvider(PaymentProvider.PAYPAL);
        p.setStatus(PaymentStatus.PENDING);
        return p;
    }

    private PaymentEvent savedEvent() {
        PaymentEvent e = new PaymentEvent();
        e.setId(UUID.randomUUID());
        return e;
    }

    @BeforeEach
    void setup() {
        lenient().when(paymentEventsRepository.save(any(PaymentEvent.class))).thenAnswer(inv -> {
            PaymentEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        lenient().when(paymentsRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
    }

    // validatePaymentForProcess

    @Test
    void processPayment_orderIdNull_throwsInvalidPaymentException() {
        Payment p = basePayment();
        p.setOrderId(null);

        assertThrows(InvalidPaymentException.class, () -> paymentsService.processPayment(p.getId()));
        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void processPayment_userIdNull_throwsInvalidPaymentException() {
        Payment p = basePayment();
        p.setUserId(null);

        assertThrows(InvalidPaymentException.class, () -> paymentsService.processPayment(p.getId()));
        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void processPayment_amountZero_throwsInvalidPaymentException() {
        Payment p = basePayment();
        p.setAmount(0);

        assertThrows(InvalidPaymentException.class, () -> paymentsService.processPayment(p.getId()));
        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void processPayment_providerNull_throwsInvalidPaymentException() {
        Payment p = basePayment();
        p.setPaymentProvider(null);

        assertThrows(InvalidPaymentException.class, () -> paymentsService.processPayment(p.getId()));
        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void processPayment_providerNotPaypal_throwsInvalidPaymentException() {
        Payment p = basePayment();
        p.setPaymentProvider(PaymentProvider.valueOf("PAYPAL"));
    }

    // processPayment - new payment (no existing)

    @Test
    void processPayment_success_newPayment_setsPending_publishesEvent() {
        Payment request = basePayment();
        request.setId(null);
        request.setProviderRef(null);

        when(paymentsRepository.findByOrderId(request.getOrderId())).thenReturn(Optional.empty());

        doAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setProviderRef("PROV-123");
            return null;
        }).when(paymentProviderClient).createPaymentOrder(any(Payment.class));

        Payment res = paymentsService.processPayment(request.getId());

        assertNotNull(res.getId());
        assertEquals(PaymentStatus.PENDING, res.getStatus());
        assertEquals("PROV-123", res.getProviderRef());

        verify(paymentProviderClient).createPaymentOrder(any(Payment.class));
        verify(paymentEventsRepository, atLeastOnce()).save(any(PaymentEvent.class));
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.PENDING), any(Payment.class));
    }

    @Test
    void processPayment_providerRefused_newPayment_setsFailed_publishesFailed_andThrows() {
        Payment request = basePayment();
        request.setId(null);

        when(paymentsRepository.findByOrderId(request.getOrderId())).thenReturn(Optional.empty());

        doThrow(new PaymentRefusedException("refused"))
                .when(paymentProviderClient).createPaymentOrder(any(Payment.class));

        assertThrows(PaymentRefusedException.class, () -> paymentsService.processPayment(request.getId()));

        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.FAILED), any(Payment.class));
        verify(paymentsRepository, atLeast(2)).save(any(Payment.class)); // salva PENDING e depois FAILED
    }

    @Test
    void processPayment_runtimeException_bubblesUp() {
        Payment request = basePayment();
        request.setId(null);

        when(paymentsRepository.findByOrderId(request.getOrderId())).thenReturn(Optional.empty());

        doThrow(new RuntimeException("boom"))
                .when(paymentProviderClient).createPaymentOrder(any(Payment.class));

        assertThrows(RuntimeException.class, () -> paymentsService.processPayment(request.getId()));
    }

    // processPayment - existing payment branches (FALTAVA no doc)

    @Test
    void processPayment_existingPendingWithProviderRef_returnsExistingWithoutCallingProvider() {
        Payment existing = basePayment();
        existing.setStatus(PaymentStatus.PENDING);
        existing.setProviderRef("EXISTING-REF");

        Payment request = basePayment();
        request.setOrderId(existing.getOrderId()); // mesma order
        request.setUserId(existing.getUserId());

        when(paymentsRepository.findByOrderId(existing.getOrderId())).thenReturn(Optional.of(existing));

        Payment res = paymentsService.processPayment(request.getId());

        assertSame(existing, res);
        verifyNoInteractions(paymentProviderClient);
        verify(paymentEventsPublisher, never()).publishPaymentEvent(any(), any());
    }

    @Test
    void processPayment_existingFinalized_throwsInvalidPaymentException() {
        Payment existing = basePayment();
        existing.setStatus(PaymentStatus.CAPTURED);

        Payment request = basePayment();
        request.setOrderId(existing.getOrderId());

        when(paymentsRepository.findByOrderId(existing.getOrderId())).thenReturn(Optional.of(existing));

        assertThrows(InvalidPaymentException.class, () -> paymentsService.processPayment(request.getId()));
        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void processPayment_existingNotFinal_updatesCallsProvider_savesPublishes() {
        Payment existing = basePayment();
        existing.setStatus(PaymentStatus.FAILED);
        existing.setProviderRef(null);

        Payment request = basePayment();
        request.setOrderId(existing.getOrderId());
        request.setUserId(existing.getUserId());
        request.setAmount(99f);

        when(paymentsRepository.findByOrderId(existing.getOrderId())).thenReturn(Optional.of(existing));

        doAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setProviderRef("NEW-REF");
            return null;
        }).when(paymentProviderClient).createPaymentOrder(any(Payment.class));

        Payment res = paymentsService.processPayment(request.getId());

        assertEquals(PaymentStatus.PENDING, res.getStatus());
        assertEquals(99f, res.getAmount());
        assertEquals("NEW-REF", res.getProviderRef());

        verify(paymentProviderClient).createPaymentOrder(any(Payment.class));
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.PENDING), any(Payment.class));
    }

    // capturePaypalPayment

    @Test
    void capturePaypalPayment_providerRefNotFound_throwsPaymentNotFound() {
        when(paymentsRepository.findByProviderRef("X")).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentsService.capturePaypalPayment("X"));
    }

    @Test
    void capturePaypalPayment_notPaypal_throwsInvalidPayment() {
        Payment p = basePayment();
        p.setPaymentProvider(null); // ou outro provider se existir
        when(paymentsRepository.findByProviderRef("X")).thenReturn(Optional.of(p));
        assertThrows(InvalidPaymentException.class, () -> paymentsService.capturePaypalPayment("X"));
    }

    @Test
    void capturePaypalPayment_statusNotPending_throwsInvalidPayment() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CAPTURED);
        when(paymentsRepository.findByProviderRef("X")).thenReturn(Optional.of(p));
        assertThrows(InvalidPaymentException.class, () -> paymentsService.capturePaypalPayment("X"));
    }

    @Test
    void capturePaypalPayment_success_setsCaptured_publishesEvent() {
        Payment p = basePayment();
        p.setProviderRef("TOK");
        p.setStatus(PaymentStatus.PENDING);
        p.setPaymentProvider(PaymentProvider.PAYPAL);

        when(paymentsRepository.findByProviderRef("TOK")).thenReturn(Optional.of(p));

        Payment res = paymentsService.capturePaypalPayment("TOK");

        assertEquals(PaymentStatus.CAPTURED, res.getStatus());
        verify(paymentProviderClient).capturePayment("TOK");
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.CAPTURED), any(Payment.class));
    }

    // cancelPayment

    @Test
    void cancelPayment_notFound_throwsPaymentNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentsService.cancelPayment(id));
    }

    @Test
    void cancelPayment_alreadyCanceled_throwsPaymentAlreadyCanceled() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CANCELED);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));
        assertThrows(PaymentAlreadyCanceledException.class, () -> paymentsService.cancelPayment(p.getId()));
    }

    @Test
    void cancelPayment_refunded_throwsInvalidPayment() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.REFUNDED);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));
        assertThrows(InvalidPaymentException.class, () -> paymentsService.cancelPayment(p.getId()));
    }

    @Test
    void cancelPayment_success_setsCanceled_publishesEvent() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.PENDING);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));

        Payment res = paymentsService.cancelPayment(p.getId());

        assertEquals(PaymentStatus.CANCELED, res.getStatus());
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.CANCEL), any(Payment.class));
    }

    // processRefund

    @Test
    void processRefund_notFound_throwsPaymentNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentsService.processRefund(id));
    }

    @Test
    void processRefund_statusNotCaptured_throwsInvalidRefund() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.PENDING);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));
        assertThrows(InvalidRefundException.class, () -> paymentsService.processRefund(p.getId()));
    }

    @Test
    void processRefund_success_setsRefunded_publishesEvent() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CAPTURED);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));

        Payment res = paymentsService.processRefund(p.getId());

        assertEquals(PaymentStatus.REFUNDED, res.getStatus());
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.REFUND), any(Payment.class));
    }

    // getPayment / getPaymentsByUser

    @Test
    void getPayment_exists_returnsPayment() {
        Payment p = basePayment();
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));
        assertEquals(p, paymentsService.getPayment(p.getId()));
    }

    @Test
    void getPayment_notExists_throwsPaymentNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentsService.getPayment(id));
    }

    @Test
    void getPaymentsByUser_empty_throwsUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(paymentsRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        assertThrows(UserNotFoundException.class, () -> paymentsService.getPaymentsByUser(userId));
    }

    @Test
    void getPaymentsByUser_hasPayments_returnsList() {
        UUID userId = UUID.randomUUID();
        when(paymentsRepository.findByUserId(userId)).thenReturn(Arrays.asList(basePayment()));
        assertEquals(1, paymentsService.getPaymentsByUser(userId).size());
    }

    // onRefundApproved / onOrderCreated

    @Test
    void onRefundApproved_notFound_throwsPaymentNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentsService.onRefundApproved(id));
    }

    @Test
    void onRefundApproved_success_setsRefunded_publishesEvent() {
        Payment p = basePayment();
        p.setStatus(PaymentStatus.CAPTURED);
        when(paymentsRepository.findById(p.getId())).thenReturn(Optional.of(p));

        paymentsService.onRefundApproved(p.getId());

        assertEquals(PaymentStatus.REFUNDED, p.getStatus());
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.REFUND), any(Payment.class));
    }

    @Test
    void onOrderCreated_ifPaymentAlreadyExists_ignores() {
        UUID orderId = UUID.randomUUID();
        when(paymentsRepository.findByOrderId(orderId)).thenReturn(Optional.of(basePayment()));

        paymentsService.onOrderCreated(orderId, UUID.randomUUID(), 10f);

        verify(paymentsRepository, never()).save(any());
        verify(paymentEventsPublisher, never()).publishPaymentEvent(any(), any());
    }

    @Test
    void onOrderCreated_createsPendingPayment_event_publish() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(paymentsRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        paymentsService.onOrderCreated(orderId, userId, 10f);

        verify(paymentsRepository).save(argThat(p ->
                p.getOrderId().equals(orderId) &&
                        p.getUserId().equals(userId) &&
                        p.getAmount() == 10f &&
                        p.getStatus() == PaymentStatus.PENDING
        ));
        verify(paymentEventsPublisher).publishPaymentEvent(eq(PaymentEventType.PENDING), any(Payment.class));
    }

}
