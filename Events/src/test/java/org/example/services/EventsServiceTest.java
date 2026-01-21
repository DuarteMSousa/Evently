package org.example.services;

import feign.FeignException;
import org.example.clients.OrganizationsClient;
import org.example.clients.TicketManagementClient;
import org.example.dtos.externalServices.organizations.OrganizationDTO;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.repositories.EventsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventsServiceTest {

    @Mock private EventsRepository eventsRepository;
    @Mock private OrganizationsClient organizationsClient;
    @Mock private TicketManagementClient ticketReservationsClient;
    @Mock private RabbitTemplate template;

    @InjectMocks private EventsService eventsService;

    private Event baseEvent() {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setName("E1");
        e.setDescription("Desc");
        e.setCreatedBy(UUID.randomUUID());
        e.setOrganizationId(UUID.randomUUID());
        return e;
    }

    @Test
    void createEvent_alreadyExists_throws() {
        Event e = baseEvent();
        when(eventsRepository.existsByName(e.getName())).thenReturn(true);

        assertThrows(EventAlreadyExistsException.class, () -> eventsService.createEvent(e));
        verify(eventsRepository, never()).save(any());
    }

    @Test
    void createEvent_emptyName_throwsInvalidEventException() {
        Event e = baseEvent();
        e.setName("");

        when(eventsRepository.existsByName("")).thenReturn(false);

        InvalidEventException ex = assertThrows(InvalidEventException.class, () -> eventsService.createEvent(e));
        assertEquals("Empty category name", ex.getMessage());
    }

    @Test
    void createEvent_emptyDescription_throwsInvalidEventException() {
        Event e = baseEvent();
        e.setDescription("");

        when(eventsRepository.existsByName(e.getName())).thenReturn(false);

        InvalidEventException ex = assertThrows(InvalidEventException.class, () -> eventsService.createEvent(e));
        assertEquals("Empty description", ex.getMessage());
    }

    @Test
    void createEvent_orgClientFeign_throwsExternalServiceException() {
        Event e = baseEvent();

        when(eventsRepository.existsByName(e.getName())).thenReturn(false);
        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("boom");
        when(organizationsClient.getOrganizationsByUser(e.getCreatedBy())).thenThrow(fe);

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> eventsService.createEvent(e));
        assertTrue(ex.getMessage().contains("OrganizationsService"));
    }

    @Test
    void createEvent_userNotInOrg_throwsInvalidEventException() {
        Event e = baseEvent();

        when(eventsRepository.existsByName(e.getName())).thenReturn(false);

        OrganizationDTO org = mock(OrganizationDTO.class);
        when(org.getId()).thenReturn(UUID.randomUUID());
        when(organizationsClient.getOrganizationsByUser(e.getCreatedBy()))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(org)));

        InvalidEventException ex = assertThrows(InvalidEventException.class, () -> eventsService.createEvent(e));
        assertTrue(ex.getMessage().contains("Organization not found"));
        verify(eventsRepository, never()).save(any());
    }

    @Test
    void createEvent_success_setsDraftAndSaves() {
        Event e = baseEvent();

        when(eventsRepository.existsByName(e.getName())).thenReturn(false);

        OrganizationDTO org = mock(OrganizationDTO.class);
        when(org.getId()).thenReturn(e.getOrganizationId());
        when(organizationsClient.getOrganizationsByUser(e.getCreatedBy()))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(org)));

        when(eventsRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event res = eventsService.createEvent(e);

        assertEquals(EventStatus.DRAFT, res.getStatus());
        verify(eventsRepository).save(e);
    }

    @Test
    void updateEvent_idMismatch_throwsInvalidEventUpdateException() {
        UUID pathId = UUID.randomUUID();
        Event payload = baseEvent();
        payload.setId(UUID.randomUUID());

        assertThrows(InvalidEventUpdateException.class, () -> eventsService.updateEvent(pathId, payload));
        verify(eventsRepository, never()).save(any());
    }

    @Test
    void updateEvent_notFound_throwsEventNotFoundException() {
        UUID id = UUID.randomUUID();
        Event payload = baseEvent();
        payload.setId(id);

        when(eventsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventsService.updateEvent(id, payload));
    }

    @Test
    void updateEvent_success_preservesStatus() {
        UUID id = UUID.randomUUID();
        Event payload = baseEvent();
        payload.setId(id);
        payload.setName("NewName");
        payload.setDescription("NewDesc");

        Event existing = baseEvent();
        existing.setId(id);
        existing.setStatus(EventStatus.PUBLISHED);

        when(eventsRepository.findById(id)).thenReturn(Optional.of(existing));

        OrganizationDTO org = mock(OrganizationDTO.class);
        when(org.getId()).thenReturn(payload.getOrganizationId());
        when(organizationsClient.getOrganizationsByUser(payload.getCreatedBy()))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(org)));

        when(eventsRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event res = eventsService.updateEvent(id, payload);

        assertEquals(EventStatus.PUBLISHED, res.getStatus());
        verify(eventsRepository).save(existing);
    }

    @Test
    void getEvent_notFound_throwsEventNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventsService.getEvent(id));
    }

    @Test
    void cancelEvent_reservationsTrue_throwsInvalidEventUpdateException() {
        UUID id = UUID.randomUUID();

        when(eventsRepository.findById(id)).thenReturn(Optional.of(baseEvent()));
        when(ticketReservationsClient.checkEventReservations(id)).thenReturn(ResponseEntity.ok(true));

        assertThrows(InvalidEventUpdateException.class, () -> eventsService.cancelEvent(id));
        verify(eventsRepository, never()).save(any());
    }

    @Test
    void cancelEvent_feign_throwsExternalServiceException() {
        UUID id = UUID.randomUUID();

        when(eventsRepository.findById(id)).thenReturn(Optional.of(baseEvent()));
        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("boom");
        when(ticketReservationsClient.checkEventReservations(id)).thenThrow(fe);

        assertThrows(ExternalServiceException.class, () -> eventsService.cancelEvent(id));
    }

    @Test
    void cancelEvent_eventNotFound_throwsEventNotFoundException() {
        UUID id = UUID.randomUUID();

        when(eventsRepository.findById(id)).thenReturn(Optional.empty());
        when(ticketReservationsClient.checkEventReservations(id)).thenReturn(ResponseEntity.ok(false));

        assertThrows(EventNotFoundException.class, () -> eventsService.cancelEvent(id));
    }

    @Test
    void cancelEvent_alreadyCanceled_throwsEventAlreadyCanceledException() {
        UUID id = UUID.randomUUID();
        Event e = baseEvent();
        e.setStatus(EventStatus.CANCELED);

        when(eventsRepository.findById(id)).thenReturn(Optional.of(e));
        when(ticketReservationsClient.checkEventReservations(id)).thenReturn(ResponseEntity.ok(false));

        assertThrows(EventNotPublishedException.class, () -> eventsService.cancelEvent(id));
    }

    @Test
    void cancelEvent_success_setsCanceled_sendsMessage_saves() {
        UUID id = UUID.randomUUID();
        Event e = baseEvent();
        e.setId(id);
        e.setStatus(EventStatus.PUBLISHED);

        when(eventsRepository.findById(id)).thenReturn(Optional.of(e));
        when(ticketReservationsClient.checkEventReservations(id)).thenReturn(ResponseEntity.ok(false));
        when(eventsRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event res = eventsService.cancelEvent(id);

        assertEquals(EventStatus.CANCELED, res.getStatus());
        verify(template).convertAndSend(any());
        verify(eventsRepository).save(e);
    }

    @Test
    void publishEvent_notFound_throwsEventNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventsRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventsService.publishEvent(id));
    }

    @Test
    void publishEvent_alreadyPublished_throwsEventAlreadyPublishedException() {
        UUID id = UUID.randomUUID();
        Event e = baseEvent();
        e.setStatus(EventStatus.PUBLISHED);

        when(eventsRepository.findById(id)).thenReturn(Optional.of(e));

        assertThrows(EventAlreadyPublishedException.class, () -> eventsService.publishEvent(id));
    }

    @Test
    void publishEvent_success_setsPending_sendsMessage_saves() {
        UUID id = UUID.randomUUID();
        Event e = baseEvent();
        e.setId(id);
        e.setStatus(EventStatus.DRAFT);

        when(eventsRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventsRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event res = eventsService.publishEvent(id);

        assertEquals(EventStatus.PENDING_STOCK_GENERATION, res.getStatus());
        verify(template).convertAndSend(any());
        verify(eventsRepository).save(e);
    }

    @Test
    void getEventPage_clampsValues_callsRepository() {
        Page<Event> page = new PageImpl<>(Collections.singletonList(baseEvent()));
        when(eventsRepository.findAllByStatus(eq(EventStatus.PUBLISHED), any())).thenReturn(page);

        Page<Event> res = eventsService.getEventPage(0, 999);

        assertEquals(1, res.getTotalElements());
        verify(eventsRepository).findAllByStatus(eq(EventStatus.PUBLISHED), argThat(pr ->
                pr.getPageNumber() == 1 && pr.getPageSize() == 50
        ));
    }
}
