package org.example.services;

import jakarta.transaction.Transactional;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class EventsService {

    @Autowired
    private EventsRepository eventsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public Event createEvent(Event event) {
        if (eventsRepository.existsByName(event.getName())) {
            throw new EventAlreadyExistsException("Event with name " + event.getName() + " already exists");
        }

        if (event.getName() == null || event.getName().isEmpty()) {
            throw new InvalidEventException("Empty category name");
        }

        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            throw new InvalidEventException("Empty description");
        }

        event.setStatus(EventStatus.DRAFT);

        //createdby para ver organization

        return eventsRepository.save(event);
    }

    @Transactional
    public Event updateEvent(UUID id, Event event) {
        if (!id.equals(event.getId())) {
            throw new InvalidEventUpdateException("Parameter id and body id do not correspond");
        }

        Event existingEvent = eventsRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        if (event.getName() == null || event.getName().isEmpty()) {
            throw new InvalidEventException("Empty category name");
        }

        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            throw new InvalidEventException("Empty description");
        }

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(event, existingEvent);

        return eventsRepository.save(existingEvent);
    }

    public Event getEvent(UUID eventId) {
        return eventsRepository
                .findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));
    }


    @Transactional
    public Event cancelEvent(UUID eventId) {
        Event eventToCancel = eventsRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(""));

        if (!eventToCancel.getStatus().equals(EventStatus.CANCELED)) {
            throw new EventAlreadyCanceledException("Event already cancelled");
        }

        //enviar mensagem
        eventToCancel.setStatus(EventStatus.CANCELED);
        return eventsRepository.save(eventToCancel);
    }

    @Transactional
    public Event publishEvent(UUID eventId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(""));

        event.setStatus(EventStatus.PENDING_STOCK_GENERATION);

        //enviar mensagem
        return eventsRepository.save(event);
    }

    public Page<Event> getEventPage(Integer pageNumber, Integer pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return eventsRepository.findAllByStatus(EventStatus.PUBLISHED, pageable);
    }
}
