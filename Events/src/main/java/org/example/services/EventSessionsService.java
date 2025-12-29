package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.EventSessionNotFoundException;
import org.example.exceptions.InvalidEventUpdateException;
import org.example.models.EventSession;
import org.example.repositories.EventSessionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventSessionsService {

    @Autowired
    private EventSessionsRepository eventSessionsRepository;

    @Transactional
    public EventSession createEventSession(EventSession eventSession) {
        //createdby para ver organization

        return eventSessionsRepository.save(eventSession);
    }

    @Transactional
    public EventSession updateEventSession(UUID id, EventSession eventSession) {
        if (!id.equals(eventSession.getId())) {
            throw new InvalidEventUpdateException("Parameter id and body id do not correspond");
        }

        EventSession existingEventSession = eventSessionsRepository.findById(id)
                .orElseThrow(() -> new EventSessionNotFoundException("Event Session not found"));


        //VERIFICAR SE ALTERA CORRETAMENTE

        return eventSessionsRepository.save(existingEventSession);
    }

    public EventSession getEventSession(UUID eventId) {
        return eventSessionsRepository
                .findById(eventId)
                .orElseThrow(() -> new EventSessionNotFoundException("Event Session not found"));
    }

    public void deleteEventSession(UUID id) {
        EventSession eventSessionToDelete = eventSessionsRepository.findById(id).orElse(null);

        if (eventSessionToDelete == null) {
            throw new EventSessionNotFoundException("Event Session not found");
        }

        eventSessionsRepository.delete(eventSessionToDelete);
    }

}
