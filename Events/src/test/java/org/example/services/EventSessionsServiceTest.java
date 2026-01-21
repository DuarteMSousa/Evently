package org.example.services;

import feign.FeignException;
import org.example.clients.TicketManagementClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.venues.VenueDTO;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.models.EventSession;
import org.example.repositories.EventSessionsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSessionsServiceTest {

    @Mock private EventSessionsRepository eventSessionsRepository;
    @Mock private EventsService eventsService;
    @Mock private TicketManagementClient ticketReservationsClient;
    @Mock private VenuesClient venuesClient;

    @InjectMocks private EventSessionsService eventSessionsService;

    private EventSession baseSession() {
        EventSession s = new EventSession();
        s.setId(UUID.randomUUID());
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setStatus(EventStatus.PUBLISHED);
        s.setEvent(e);
        s.setVenueId(UUID.randomUUID());
        s.setStartsAt(Instant.now().plus(1, ChronoUnit.DAYS));
        s.setEndsAt(Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
        return s;
    }

    @Test
    void createEventSession_invalidDatesNull_throwsInvalidEventSessionException() {
        EventSession s = baseSession();
        s.setStartsAt(null);

        assertThrows(InvalidEventSessionException.class, () -> eventSessionsService.createEventSession(s));
        verify(eventSessionsRepository, never()).save(any());
    }

    @Test
    void createEventSession_endBeforeStart_throwsInvalidEventSessionException() {
        EventSession s = baseSession();
        s.setEndsAt(s.getStartsAt().minus(1, ChronoUnit.HOURS));

        assertThrows(InvalidEventSessionException.class, () -> eventSessionsService.createEventSession(s));
    }

    @Test
    void createEventSession_eventNotFound_throwsInvalidEventSessionException() {
        EventSession s = baseSession();
        when(eventsService.getEvent(s.getEvent().getId())).thenThrow(new EventNotFoundException("Event not found"));

        InvalidEventSessionException ex = assertThrows(InvalidEventSessionException.class,
                () -> eventSessionsService.createEventSession(s));

        assertEquals("Event not found", ex.getMessage());
    }

    @Test
    void createEventSession_venueNotFound_throwsInvalidEventSessionException() {
        EventSession s = baseSession();
        when(eventsService.getEvent(s.getEvent().getId())).thenReturn(new Event());

        FeignException.NotFound nf = mock(FeignException.NotFound.class);
        when(nf.contentUTF8()).thenReturn("notfound");
        when(venuesClient.getVenue(s.getVenueId())).thenThrow(nf);

        InvalidEventSessionException ex = assertThrows(InvalidEventSessionException.class,
                () -> eventSessionsService.createEventSession(s));

        assertEquals("Venue not found", ex.getMessage());
    }

    @Test
    void createEventSession_venueFeign_throwsExternalServiceException() {
        EventSession s = baseSession();
        when(eventsService.getEvent(s.getEvent().getId())).thenReturn(new Event());

        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("boom");
        when(venuesClient.getVenue(s.getVenueId())).thenThrow(fe);

        assertThrows(ExternalServiceException.class, () -> eventSessionsService.createEventSession(s));
    }

    @Test
    void createEventSession_success_saves() {
        EventSession s = baseSession();

        when(eventsService.getEvent(s.getEvent().getId())).thenReturn(new Event());

        VenueDTO venue = mock(VenueDTO.class);
        when(venuesClient.getVenue(s.getVenueId())).thenReturn(ResponseEntity.ok(venue));

        when(eventSessionsRepository.save(any(EventSession.class))).thenAnswer(inv -> inv.getArgument(0));

        EventSession res = eventSessionsService.createEventSession(s);

        assertEquals(s.getVenueId(), res.getVenueId());
        verify(eventSessionsRepository).save(s);
    }

    @Test
    void updateEventSession_idMismatch_throwsInvalidEventSessionUpdateException() {
        UUID path = UUID.randomUUID();
        EventSession payload = baseSession();
        payload.setId(UUID.randomUUID());

        assertThrows(InvalidEventSessionUpdateException.class, () -> eventSessionsService.updateEventSession(path, payload));
    }

    @Test
    void updateEventSession_notFound_throwsEventSessionNotFoundException() {
        UUID id = UUID.randomUUID();
        EventSession payload = baseSession();
        payload.setId(id);

        when(eventSessionsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventSessionNotFoundException.class, () -> eventSessionsService.updateEventSession(id, payload));
    }

    @Test
    void updateEventSession_success_updatesAndSaves() {
        UUID id = UUID.randomUUID();

        EventSession payload = baseSession();
        payload.setId(id);

        EventSession existing = baseSession();
        existing.setId(id);

        when(eventSessionsRepository.findById(id))
                .thenReturn(Optional.of(existing));

        when(eventsService.getEvent(any(UUID.class)))
                .thenReturn(new Event());

        VenueDTO venue = mock(VenueDTO.class);
        when(venuesClient.getVenue(any(UUID.class)))
                .thenReturn(ResponseEntity.ok(venue));

        when(eventSessionsRepository.save(any(EventSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EventSession res = eventSessionsService.updateEventSession(id, payload);

        assertNotNull(res);
        assertEquals(payload.getStartsAt(), res.getStartsAt());
        assertEquals(payload.getEndsAt(), res.getEndsAt());

        verify(eventSessionsRepository).save(existing);

        verify(eventsService).getEvent(any(UUID.class));
    }

    @Test
    void getEventSession_notFound_throwsEventSessionNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventSessionsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventSessionNotFoundException.class, () -> eventSessionsService.getEventSession(id));
    }

    @Test
    void deleteEventSession_notFound_throwsEventSessionNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventSessionsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventSessionNotFoundException.class, () -> eventSessionsService.deleteEventSession(id));
    }

    @Test
    void deleteEventSession_feign_throwsExternalServiceException() {
        UUID id = UUID.randomUUID();
        EventSession s = baseSession();
        s.setId(id);

        when(eventSessionsRepository.findById(id)).thenReturn(Optional.of(s));

        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("boom");
        when(ticketReservationsClient.checkSessionReservations(id)).thenThrow(fe);

        assertThrows(ExternalServiceException.class, () -> eventSessionsService.deleteEventSession(id));
    }

    @Test
    void deleteEventSession_hasReservations_throwsInvalidEventSessionUpdateException() {
        UUID id = UUID.randomUUID();
        EventSession s = baseSession();
        s.setId(id);

        when(eventSessionsRepository.findById(id)).thenReturn(Optional.of(s));
        when(ticketReservationsClient.checkSessionReservations(id)).thenReturn(ResponseEntity.ok(true));

        assertThrows(InvalidEventSessionUpdateException.class, () -> eventSessionsService.deleteEventSession(id));
        verify(eventSessionsRepository, never()).delete(any());
    }

    @Test
    void deleteEventSession_success_deletes() {
        UUID id = UUID.randomUUID();
        EventSession s = baseSession();
        s.setId(id);

        when(eventSessionsRepository.findById(id)).thenReturn(Optional.of(s));
        when(ticketReservationsClient.checkSessionReservations(id)).thenReturn(ResponseEntity.ok(false));

        eventSessionsService.deleteEventSession(id);

        verify(eventSessionsRepository).delete(s);
    }

}
