package org.evently.orders.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.evently.orders.clients.EventsClient;
import org.evently.orders.clients.TicketManagementClient;
import org.evently.orders.dtos.externalServices.events.EventSessionDTO;
import org.evently.orders.dtos.externalServices.events.SessionTierDTO;
import org.evently.orders.dtos.externalServices.ticketManagement.TicketReservationCreateDTO;
import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.ExternalServiceException;
import org.evently.orders.exceptions.InvalidOrderException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.exceptions.externalServices.ProductNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.models.OrderLine;
import org.evently.orders.models.OrderLineId;
import org.evently.orders.publishers.OrdersEventsPublisher;
import org.evently.orders.repositories.OrdersRepository;
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
class OrdersServiceTest {

    @Mock private OrdersRepository ordersRepository;
    @Mock private EventsClient eventsClient;
    @Mock private TicketManagementClient ticketManagementClient;
    @Mock private OrdersEventsPublisher ordersEventsPublisher;

    @InjectMocks private OrdersService ordersService;

    private UUID userId;
    private UUID productId;
    private UUID orderId;
    private UUID eventSessionId;
    private UUID eventId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        eventSessionId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    // -------- Helpers --------

    private Order baseOrderWithOneLine(int qty) {
        Order o = new Order();
        o.setId(orderId);
        o.setUserId(userId);
        o.setStatus(OrderStatus.CREATED);
        o.setTotal(0);

        OrderLine line = new OrderLine();
        line.setId(new OrderLineId(null, productId));
        line.setQuantity(qty);
        line.setUnitPrice(0);
        line.setOrder(o);

        o.setLines(new ArrayList<>(Collections.singletonList(line)));
        return o;
    }

    private SessionTierDTO tier(UUID tierId, float price) {
        SessionTierDTO t = new SessionTierDTO();
        t.setId(tierId);
        t.setPrice(price);
        t.setEventSessionId(eventSessionId);
        t.setZoneId(UUID.randomUUID());
        return t;
    }

    private EventSessionDTO eventSession() {
        EventSessionDTO s = new EventSessionDTO();
        s.setId(eventSessionId);
        s.setEventId(eventId);
        return s;
    }

    private FeignException feignNotFound() {
        Request req = Request.create(
                Request.HttpMethod.GET,
                "/x",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        return new FeignException.NotFound(
                "not found",
                req,
                null,
                new HashMap<>()
        );
    }

    private FeignException feignGeneric500() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", new HashMap<>(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().request(req).status(500).reason("Error").headers(new HashMap<>()).build();
        return FeignException.errorStatus("op", resp);
    }

    // -------- getOrder --------

    @Test
    void getOrder_exists_returnsOrder() {
        Order o = new Order();
        o.setId(orderId);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));

        Order res = ordersService.getOrder(orderId);

        assertEquals(orderId, res.getId());
        verify(ordersRepository).findById(orderId);
    }

    @Test
    void getOrder_missing_throwsOrderNotFound() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () -> ordersService.getOrder(orderId));
        assertEquals("Order not found", ex.getMessage());
    }

    // -------- getOrdersByUser (clamp pageSize/pageNumber) --------

    @Test
    void getOrdersByUser_pageSizeTooBig_clampsTo50_andPageNumberLessThan1_becomes0() {
        UUID u = UUID.randomUUID();
        when(ordersRepository.findAllByUserId(eq(u), any())).thenReturn(PageFake.onePage());

        ordersService.getOrdersByUser(u, 0, 999);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);

        verify(ordersRepository).findAllByUserId(eq(u), captor.capture());

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    // -------- createOrder (full path) --------

    @Test
    void createOrder_success_setsPricesTotalDates_reservesTickets_publishesEvent() {
        Order input = baseOrderWithOneLine(2);
        UUID tierId = productId; // productId == tierId in your logic
        SessionTierDTO tier = tier(tierId, 10.0f);

        when(eventsClient.getSessionTier(tierId)).thenReturn(ResponseEntity.ok(tier));
        when(eventsClient.getEventSession(eventSessionId)).thenReturn(ResponseEntity.ok(eventSession()));

        // repository save returns persisted order with id and lines (typical)
        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(orderId);
            // garantir que linhas ficam com order setado
            if (saved.getLines() != null) {
                for (OrderLine l : saved.getLines()) {
                    l.setOrder(saved);
                    if (l.getId() != null && l.getId().getOrderId() == null) {
                        l.getId().setOrderId(saved.getId());
                    }
                }
            }
            return saved;
        });

        Order res = ordersService.createOrder(input);

        // unitPrice e total calculados
        assertNotNull(res.getCreatedAt());
        assertEquals(OrderStatus.CREATED, res.getStatus());
        assertEquals(20.0f, res.getTotal(), 0.0001);

        OrderLine savedLine = res.getLines().get(0);
        assertEquals(10.0f, savedLine.getUnitPrice(), 0.0001);

        // reserva feita
        ArgumentCaptor<TicketReservationCreateDTO> reservationCaptor =
                ArgumentCaptor.forClass(TicketReservationCreateDTO.class);
        verify(ticketManagementClient).reserveTicket(reservationCaptor.capture());
        TicketReservationCreateDTO dto = reservationCaptor.getValue();
        assertEquals(userId, dto.getUserId());
        assertEquals(orderId, dto.getOrderId());
        assertEquals(eventSessionId, dto.getSessionId());
        assertEquals(tierId, dto.getTierId());
        assertEquals(eventId, dto.getEventId());
        assertEquals(2, dto.getQuantity());

        // publisher chamado
        verify(ordersEventsPublisher).publishOrderCreatedEvent(any(Order.class));
    }

    @Test
    void createOrder_eventsTierNotFound_throwsProductNotFound() {
        Order input = baseOrderWithOneLine(1);

        when(eventsClient.getSessionTier(productId)).thenThrow(feignNotFound());

        assertThrows(ProductNotFoundException.class, () -> ordersService.createOrder(input));
        verify(ordersRepository, never()).save(any());
        verify(ticketManagementClient, never()).reserveTicket(any());
        verify(ordersEventsPublisher, never()).publishOrderCreatedEvent(any());
    }

    @Test
    void createOrder_eventsTierFeignGeneric_throwsExternalServiceException() {
        Order input = baseOrderWithOneLine(1);
        when(eventsClient.getSessionTier(productId)).thenThrow(feignGeneric500());

        assertThrows(ExternalServiceException.class, () -> ordersService.createOrder(input));
        verify(ordersRepository, never()).save(any());
    }

    @Test
    void createOrder_eventsTierReturnsNullBody_throwsProductNotFound() {
        Order input = baseOrderWithOneLine(1);
        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(null));

        assertThrows(ProductNotFoundException.class, () -> ordersService.createOrder(input));
        verify(ordersRepository, never()).save(any());
    }

    @Test
    void createOrder_invalidQuantity_throwsInvalidOrderException() {
        Order input = baseOrderWithOneLine(0);

        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(tier(productId, 10.0f)));

        assertThrows(InvalidOrderException.class, () -> ordersService.createOrder(input));
        verify(ordersRepository, never()).save(any());
    }

    @Test
    void createOrder_eventSessionFeignFails_throwsExternalServiceException() {
        Order input = baseOrderWithOneLine(1);
        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(tier(productId, 10.0f)));

        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            saved.setId(orderId);
            return saved;
        });

        when(eventsClient.getEventSession(eventSessionId)).thenThrow(feignGeneric500());

        assertThrows(ExternalServiceException.class, () -> ordersService.createOrder(input));
        verify(ticketManagementClient, never()).reserveTicket(any());
        verify(ordersEventsPublisher, never()).publishOrderCreatedEvent(any());
    }

    @Test
    void createOrder_ticketManagementFeignFails_throwsExternalServiceException() {
        Order input = baseOrderWithOneLine(1);
        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(tier(productId, 10.0f)));
        when(eventsClient.getEventSession(eventSessionId)).thenReturn(ResponseEntity.ok(eventSession()));

        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            saved.setId(orderId);
            return saved;
        });

        doThrow(feignGeneric500()).when(ticketManagementClient).reserveTicket(any());

        assertThrows(ExternalServiceException.class, () -> ordersService.createOrder(input));
        verify(ordersEventsPublisher, never()).publishOrderCreatedEvent(any());
    }

    // -------- markAsPaid --------

    @Test
    void markAsPaid_success_changesStatus_setsPaidAt_publishes() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.CREATED);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order res = ordersService.markAsPaid(orderId);

        assertEquals(OrderStatus.PAYMENT_SUCCESS, res.getStatus());
        assertNotNull(res.getPaidAt());
        verify(ordersEventsPublisher).publishOrderPaidEvent(any(Order.class));
    }

    @Test
    void markAsPaid_invalidStatus_throwsInvalidOrderException() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.PAYMENT_FAILED);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));

        assertThrows(InvalidOrderException.class, () -> ordersService.markAsPaid(orderId));
        verify(ordersRepository, never()).save(any());
    }

    // -------- markAsPaymentFailed --------

    @Test
    void markAsPaymentFailed_success_changesStatus() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.CREATED);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order res = ordersService.markAsPaymentFailed(orderId);

        assertEquals(OrderStatus.PAYMENT_FAILED, res.getStatus());
    }

    @Test
    void markAsPaymentFailed_invalidStatus_throwsInvalidOrderException() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.PAYMENT_SUCCESS);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));

        assertThrows(InvalidOrderException.class, () -> ordersService.markAsPaymentFailed(orderId));
    }

    // -------- cancelOrder --------

    @Test
    void cancelOrder_success_fromCreated_setsCanceled() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.CREATED);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(ordersRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order res = ordersService.cancelOrder(orderId, false);

        assertEquals(OrderStatus.CANCELED, res.getStatus());
        assertNotNull(res.getCanceledAt());
    }

    @Test
    void cancelOrder_alreadyCanceled_throwsInvalidOrderException() {
        Order o = new Order();
        o.setId(orderId);
        o.setStatus(OrderStatus.CANCELED);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(o));

        assertThrows(InvalidOrderException.class, () -> ordersService.cancelOrder(orderId, false));
    }

    static class PageFake {
        static org.springframework.data.domain.Page<Order> onePage() {
            return new org.springframework.data.domain.PageImpl<>(Collections.singletonList(new Order()));
        }
    }
}