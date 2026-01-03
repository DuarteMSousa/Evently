package org.example.services;

import feign.FeignException;
import org.example.clients.TicketManagementClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.venueszone.VenueZoneDTO;
import org.example.exceptions.*;
import org.example.models.EventSession;
import org.example.models.SessionTier;
import org.example.repositories.SessionTiersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionTiersServiceTest {

    @Mock private SessionTiersRepository sessionTiersRepository;
    @Mock private EventSessionsService eventSessionsService;
    @Mock private VenuesClient venuesClient;
    @Mock private TicketManagementClient ticketReservationsClient;

    @InjectMocks private SessionTiersService sessionTiersService;

    private SessionTier baseTier(UUID venueId) {
        SessionTier t = new SessionTier();
        t.setId(UUID.randomUUID());
        t.setPrice(10.0f);
        t.setZoneId(UUID.randomUUID());

        EventSession s = new EventSession();
        s.setId(UUID.randomUUID());
        s.setVenueId(venueId);
        t.setEventSession(s);

        return t;
    }

    @Test
    void createSessionTier_alreadyExists_throws() {
        SessionTier t = baseTier(UUID.randomUUID());

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(true);

        assertThrows(SessionTierAlreadyExistsException.class, () -> sessionTiersService.createSessionTier(t));
        verify(sessionTiersRepository, never()).save(any());
    }

    @Test
    void createSessionTier_invalidPrice_throwsInvalidSessionTierException() {
        SessionTier t = baseTier(UUID.randomUUID());
        t.setPrice(0);

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);

        InvalidSessionTierException ex = assertThrows(InvalidSessionTierException.class,
                () -> sessionTiersService.createSessionTier(t));

        assertEquals("Session tier price must be greater than 0", ex.getMessage());
    }

    @Test
    void createSessionTier_eventSessionNotFound_throwsInvalidSessionTierException() {
        SessionTier t = baseTier(UUID.randomUUID());

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);
        when(eventSessionsService.getEventSession(t.getEventSession().getId()))
                .thenThrow(new EventSessionNotFoundException("Event Session not found"));

        InvalidSessionTierException ex = assertThrows(InvalidSessionTierException.class,
                () -> sessionTiersService.createSessionTier(t));

        assertEquals("Event session not found", ex.getMessage());
    }

    @Test
    void createSessionTier_zoneNotFound_throwsInvalidSessionTierException() {
        UUID venueId = UUID.randomUUID();
        SessionTier t = baseTier(venueId);

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);

        EventSession session = new EventSession();
        session.setId(t.getEventSession().getId());
        session.setVenueId(venueId);

        when(eventSessionsService.getEventSession(t.getEventSession().getId())).thenReturn(session);

        FeignException.NotFound nf = mock(FeignException.NotFound.class);
        when(nf.contentUTF8()).thenReturn("notfound");
        when(venuesClient.getZone(t.getZoneId())).thenThrow(nf);

        InvalidSessionTierException ex = assertThrows(InvalidSessionTierException.class,
                () -> sessionTiersService.createSessionTier(t));

        assertEquals("Venue zone not found", ex.getMessage());
    }

    @Test
    void createSessionTier_zoneFeign_throwsExternalServiceException() {
        UUID venueId = UUID.randomUUID();
        SessionTier t = baseTier(venueId);

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);

        EventSession session = new EventSession();
        session.setId(t.getEventSession().getId());
        session.setVenueId(venueId);

        when(eventSessionsService.getEventSession(t.getEventSession().getId())).thenReturn(session);

        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("boom");
        when(venuesClient.getZone(t.getZoneId())).thenThrow(fe);

        assertThrows(ExternalServiceException.class, () -> sessionTiersService.createSessionTier(t));
    }

    @Test
    void createSessionTier_zoneVenueMismatch_throwsInvalidSessionTierException() {
        UUID venueId = UUID.randomUUID();
        SessionTier t = baseTier(venueId);

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);

        EventSession session = new EventSession();
        session.setId(t.getEventSession().getId());
        session.setVenueId(venueId);
        when(eventSessionsService.getEventSession(t.getEventSession().getId())).thenReturn(session);

        VenueZoneDTO zone = mock(VenueZoneDTO.class);
        when(zone.getVenueId()).thenReturn(UUID.randomUUID()); // diferente do venueId
        when(venuesClient.getZone(t.getZoneId())).thenReturn(ResponseEntity.ok(zone));

        InvalidSessionTierException ex = assertThrows(InvalidSessionTierException.class,
                () -> sessionTiersService.createSessionTier(t));

        assertEquals("Invalid zone, does not correspond to venue", ex.getMessage());
    }

    @Test
    void createSessionTier_success_saves() {
        UUID venueId = UUID.randomUUID();
        SessionTier t = baseTier(venueId);

        when(sessionTiersRepository.existsByEventSessionAndZoneId(t.getEventSession(), t.getZoneId())).thenReturn(false);

        EventSession session = new EventSession();
        session.setId(t.getEventSession().getId());
        session.setVenueId(venueId);
        when(eventSessionsService.getEventSession(t.getEventSession().getId())).thenReturn(session);

        VenueZoneDTO zone = mock(VenueZoneDTO.class);
        when(zone.getVenueId()).thenReturn(venueId);
        when(venuesClient.getZone(t.getZoneId())).thenReturn(ResponseEntity.ok(zone));

        when(sessionTiersRepository.save(any(SessionTier.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionTier res = sessionTiersService.createSessionTier(t);

        assertEquals(t.getZoneId(), res.getZoneId());
        verify(sessionTiersRepository).save(t);
    }

    @Test
    void updateSessionTier_idMismatch_throwsInvalidSessionTierUpdateException() {
        UUID path = UUID.randomUUID();
        SessionTier t = baseTier(UUID.randomUUID());
        t.setId(UUID.randomUUID());

        assertThrows(InvalidSessionTierUpdateException.class, () -> sessionTiersService.updateSessionTier(path, t));
    }

    @Test
    void updateSessionTier_notFound_throwsSessionTierNotFoundException() {
        UUID id = UUID.randomUUID();
        SessionTier t = baseTier(UUID.randomUUID());
        t.setId(id);

        when(sessionTiersRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(SessionTierNotFoundException.class, () -> sessionTiersService.updateSessionTier(id, t));
    }

    @Test
    void deleteSessionTier_notFound_throwsSessionTierNotFoundException() {
        UUID id = UUID.randomUUID();
        when(sessionTiersRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(SessionTierNotFoundException.class, () -> sessionTiersService.deleteSessionTier(id));
    }

    @Test
    void deleteSessionTier_hasReservations_throwsInvalidEventSessionUpdateException() {
        UUID id = UUID.randomUUID();
        SessionTier t = baseTier(UUID.randomUUID());
        t.setId(id);

        when(sessionTiersRepository.findById(id)).thenReturn(Optional.of(t));
        when(ticketReservationsClient.checkTierReservations(id)).thenReturn(ResponseEntity.ok(true));

        assertThrows(InvalidEventSessionUpdateException.class, () -> sessionTiersService.deleteSessionTier(id));
        verify(sessionTiersRepository, never()).delete(any());
    }

    @Test
    void deleteSessionTier_success_deletes() {
        UUID id = UUID.randomUUID();
        SessionTier t = baseTier(UUID.randomUUID());
        t.setId(id);

        when(sessionTiersRepository.findById(id)).thenReturn(Optional.of(t));
        when(ticketReservationsClient.checkTierReservations(id)).thenReturn(ResponseEntity.ok(false));

        sessionTiersService.deleteSessionTier(id);

        verify(sessionTiersRepository).delete(t);
    }
}
