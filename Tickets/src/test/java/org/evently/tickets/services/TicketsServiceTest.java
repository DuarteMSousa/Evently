package org.evently.tickets.services;

import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.publishers.TicketsEventsPublisher;
import org.evently.tickets.repositories.TicketsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketsServiceTest {

    @Mock private TicketsRepository ticketsRepository;
    @Mock private TicketsEventsPublisher ticketsEventsPublisher;

    @InjectMocks private TicketsService ticketsService;

    private Ticket baseTicket;

    @BeforeEach
    void setup() {
        baseTicket = new Ticket();
        baseTicket.setId(UUID.randomUUID());
        baseTicket.setReservationId(UUID.randomUUID());
        baseTicket.setOrderId(UUID.randomUUID());
        baseTicket.setUserId(UUID.randomUUID());
        baseTicket.setEventId(UUID.randomUUID());
        baseTicket.setSessionId(UUID.randomUUID());
        baseTicket.setTierId(UUID.randomUUID());
        baseTicket.setStatus(TicketStatus.ISSUED);
        baseTicket.setIssuedAt(new Date());
    }

    // -------------------------
    // getTicket
    // -------------------------

    @Test
    void getTicket_exists_returnsTicket() {
        UUID id = UUID.randomUUID();
        when(ticketsRepository.findById(id)).thenReturn(Optional.of(baseTicket));

        Ticket res = ticketsService.getTicket(id);

        assertSame(baseTicket, res);
        verify(ticketsRepository).findById(id);
    }

    @Test
    void getTicket_notExists_throwsTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketsService.getTicket(id));
        verify(ticketsRepository).findById(id);
    }

    // -------------------------
    // issueTicket validations
    // -------------------------

    @Test
    void issueTicket_reservationIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setReservationId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_orderIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setOrderId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_userIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setUserId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_eventIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setEventId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_sessionIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setSessionId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_tierIdNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setTierId(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_statusNull_throwsInvalidTicketUpdate() {
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(null);

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.issueTicket(t));
        verify(ticketsRepository, never()).save(any());
        verifyNoInteractions(ticketsEventsPublisher);
    }

    @Test
    void issueTicket_valid_savesAndPublishes() {
        Ticket input = cloneTicket(baseTicket);
        input.setId(null);

        Ticket saved = cloneTicket(baseTicket);
        saved.setId(UUID.randomUUID());

        when(ticketsRepository.save(any(Ticket.class))).thenReturn(saved);

        Ticket res = ticketsService.issueTicket(input);

        assertEquals(saved.getId(), res.getId());
        verify(ticketsRepository).save(any(Ticket.class));
        verify(ticketsEventsPublisher).publishTicketIssuedEvent(saved);
    }

    // -------------------------
    // cancelTicket
    // -------------------------

    @Test
    void cancelTicket_notFound_throwsTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketsService.cancelTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void cancelTicket_used_throwsInvalidTicketUpdate() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.USED);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.cancelTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void cancelTicket_cancelled_throwsInvalidTicketUpdate() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.CANCELLED);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.cancelTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void cancelTicket_issued_setsCancelledAndSaves() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.ISSUED);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));
        when(ticketsRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket res = ticketsService.cancelTicket(id);

        assertEquals(TicketStatus.CANCELLED, res.getStatus());
        verify(ticketsRepository).save(t);
    }

    // -------------------------
    // useTicket
    // -------------------------

    @Test
    void useTicket_notFound_throwsTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketsService.useTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void useTicket_used_throwsInvalidTicketUpdate() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.USED);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.useTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void useTicket_cancelled_throwsInvalidTicketUpdate() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.CANCELLED);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));

        assertThrows(InvalidTicketUpdateException.class, () -> ticketsService.useTicket(id));
        verify(ticketsRepository, never()).save(any());
    }

    @Test
    void useTicket_issued_setsUsedAndValidatedAtAndSaves() {
        UUID id = UUID.randomUUID();
        Ticket t = cloneTicket(baseTicket);
        t.setStatus(TicketStatus.ISSUED);
        t.setValidatedAt(null);

        when(ticketsRepository.findById(id)).thenReturn(Optional.of(t));
        when(ticketsRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket res = ticketsService.useTicket(id);

        assertEquals(TicketStatus.USED, res.getStatus());
        assertNotNull(res.getValidatedAt());
        verify(ticketsRepository).save(t);
    }

    // -------------------------
    // getTicketsByUser
    // -------------------------

    @Test
    void getTicketsByUser_pageSizeTooBig_clampsTo50() {
        UUID userId = UUID.randomUUID();
        Page<Ticket> page = new PageImpl<>(java.util.Collections.singletonList(baseTicket));

        when(ticketsRepository.findAllByUserId(eq(userId), any(PageRequest.class))).thenReturn(page);

        ticketsService.getTicketsByUser(userId, 1, 999);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(ticketsRepository).findAllByUserId(eq(userId), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getTicketsByUser_pageSizeTooSmall_clampsTo50() {
        UUID userId = UUID.randomUUID();
        Page<Ticket> page = new PageImpl<>(java.util.Collections.singletonList(baseTicket));

        when(ticketsRepository.findAllByUserId(eq(userId), any(PageRequest.class))).thenReturn(page);

        ticketsService.getTicketsByUser(userId, 1, 0);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(ticketsRepository).findAllByUserId(eq(userId), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getTicketsByUser_pageNumberLessThan1_setsTo0() {
        UUID userId = UUID.randomUUID();
        Page<Ticket> page = new PageImpl<>(java.util.Collections.singletonList(baseTicket));

        when(ticketsRepository.findAllByUserId(eq(userId), any(PageRequest.class))).thenReturn(page);

        ticketsService.getTicketsByUser(userId, 0, 10);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(ticketsRepository).findAllByUserId(eq(userId), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void getTicketsByUser_normal_callsRepoWithGivenPage() {
        UUID userId = UUID.randomUUID();
        Page<Ticket> page = new PageImpl<>(java.util.Collections.singletonList(baseTicket));

        when(ticketsRepository.findAllByUserId(eq(userId), any(PageRequest.class))).thenReturn(page);

        ticketsService.getTicketsByUser(userId, 1, 10);

        verify(ticketsRepository).findAllByUserId(eq(userId), eq(PageRequest.of(1, 10)));
    }

    // helper
    private Ticket cloneTicket(Ticket t) {
        Ticket c = new Ticket();
        c.setId(t.getId());
        c.setReservationId(t.getReservationId());
        c.setOrderId(t.getOrderId());
        c.setUserId(t.getUserId());
        c.setEventId(t.getEventId());
        c.setSessionId(t.getSessionId());
        c.setTierId(t.getTierId());
        c.setStatus(t.getStatus());
        c.setIssuedAt(t.getIssuedAt());
        c.setValidatedAt(t.getValidatedAt());
        return c;
    }
}