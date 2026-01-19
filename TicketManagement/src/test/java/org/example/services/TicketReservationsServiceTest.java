package org.example.services;

import org.example.enums.StockMovementType;
import org.example.enums.TicketReservationStatus;
import org.example.exceptions.InvalidTicketReservationException;
import org.example.exceptions.TicketReservationNotFoundException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.messages.received.OrderPaidMessage;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.publishers.EventTicketManagementMessagesPublisher;
import org.example.repositories.TicketReservationsRepository;
import org.example.repositories.TicketStocksRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketReservationsServiceTest {

    @Mock private EventTicketManagementMessagesPublisher ticketManagementMessagesPublisher;
    @Mock private TicketReservationsRepository ticketReservationsRepository;
    @Mock private TicketStocksService ticketStocksService;
    @Mock private TicketStocksRepository ticketStocksRepository;

    @InjectMocks private TicketReservationsService ticketReservationsService;

    private TicketReservation valid;

    @BeforeEach
    void setup() {
        valid = new TicketReservation();
        valid.setUserId(UUID.randomUUID());
        valid.setOrderId(UUID.randomUUID());
        valid.setEventId(UUID.randomUUID());
        valid.setSessionId(UUID.randomUUID());
        valid.setTierId(UUID.randomUUID());
        valid.setQuantity(2);
    }

    @Test
    void createTicketReservation_quantityInvalid_throws() {
        valid.setQuantity(0);
        InvalidTicketReservationException ex = assertThrows(InvalidTicketReservationException.class,
                () -> ticketReservationsService.createTicketReservation(valid));
        assertEquals("Quantity must be greater than 0", ex.getMessage());
        verify(ticketReservationsRepository, never()).save(any());
    }

    @Test
    void createTicketReservation_stockNotFound_throws() {
        TicketStockId stockId = new TicketStockId(valid.getEventId(), valid.getSessionId(), valid.getTierId());
        when(ticketStocksRepository.findById(stockId)).thenReturn(Optional.empty());

        TicketStockNotFoundException ex = assertThrows(TicketStockNotFoundException.class,
                () -> ticketReservationsService.createTicketReservation(valid));
        assertEquals("Ticket Stock not found", ex.getMessage());
        verify(ticketReservationsRepository, never()).save(any());
    }

    @Test
    void createTicketReservation_success_setsHeld_andCreatesOutMovement_andSaves() {
        TicketStock stock = new TicketStock();
        stock.setId(new TicketStockId(valid.getEventId(), valid.getSessionId(), valid.getTierId()));
        when(ticketStocksRepository.findById(any(TicketStockId.class))).thenReturn(Optional.of(stock));

        when(ticketReservationsRepository.save(any(TicketReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketReservation saved = ticketReservationsService.createTicketReservation(valid);

        assertEquals(TicketReservationStatus.HELD, saved.getStatus());
        assertNull(saved.getConfirmedAt());
        verify(ticketStocksService).addStockMovement(argThat(m -> m.getType() == StockMovementType.OUT && m.getQuantity().equals(2)));
        verify(ticketReservationsRepository).save(any(TicketReservation.class));
    }

    @Test
    void confirmTicketReservation_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(ticketReservationsRepository.findById(id)).thenReturn(Optional.empty());

        TicketReservationNotFoundException ex = assertThrows(TicketReservationNotFoundException.class,
                () -> ticketReservationsService.confirmTicketReservation(id));
        assertEquals("Ticket Reservation not found", ex.getMessage());
    }

    @Test
    void confirmTicketReservation_alreadyConfirmed_throws() {
        UUID id = UUID.randomUUID();
        TicketReservation existing = new TicketReservation();
        existing.setId(id);
        existing.setStatus(TicketReservationStatus.CONFIRMED);

        when(ticketReservationsRepository.findById(id)).thenReturn(Optional.of(existing));

        InvalidTicketReservationException ex = assertThrows(InvalidTicketReservationException.class,
                () -> ticketReservationsService.confirmTicketReservation(id));
        assertEquals("Ticket Reservation status is CONFIRMED already", ex.getMessage());
        verify(ticketReservationsRepository, never()).save(any());
    }

    @Test
    void releaseTicketReservation_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(ticketReservationsRepository.findById(id)).thenReturn(Optional.empty());

        TicketReservationNotFoundException ex = assertThrows(TicketReservationNotFoundException.class,
                () -> ticketReservationsService.releaseTicketReservation(id));
        assertEquals("Ticket Reservation not found", ex.getMessage());
    }

    @Test
    void handleOrderPaid_confirmsAll_andPublishesMessages() {
        UUID orderId = UUID.randomUUID();

        TicketReservation r1 = new TicketReservation(); r1.setId(UUID.randomUUID()); r1.setOrderId(orderId); r1.setStatus(TicketReservationStatus.HELD);
        TicketReservation r2 = new TicketReservation(); r2.setId(UUID.randomUUID()); r2.setOrderId(orderId); r2.setStatus(TicketReservationStatus.HELD);

        when(ticketReservationsRepository.findByOrderId(orderId)).thenReturn(Arrays.asList(r1, r2));
        when(ticketReservationsRepository.save(any(TicketReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPaidMessage msg = new OrderPaidMessage();
        msg.setId(orderId);

        ticketReservationsService.handleOrderPaid(msg);

        verify(ticketReservationsRepository, times(2)).save(any(TicketReservation.class));
        verify(ticketManagementMessagesPublisher, times(2)).publishTicketReservationConfirmedMessage(any(TicketReservation.class));
        assertEquals(TicketReservationStatus.CONFIRMED, r1.getStatus());
        assertNotNull(r1.getConfirmedAt());
    }
}