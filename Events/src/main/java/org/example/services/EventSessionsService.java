package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.EventSessionNotFoundException;
import org.example.exceptions.InvalidEventUpdateException;
import org.example.models.EventSession;
import org.example.repositories.EventSessionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventSessionsService {

    @Autowired
    private EventSessionsRepository eventSessionsRepository;

    private Logger logger = LoggerFactory.getLogger(CategoriesService.class);

    private static final Marker SESSION_GET = MarkerFactory.getMarker("SESSION_GET");
    private static final Marker SESSION_DELETE = MarkerFactory.getMarker("SESSION_DELETE");
    private static final Marker SESSION_UPDATE = MarkerFactory.getMarker("SESSION_UPDATE");
    private static final Marker SESSION_CREATE = MarkerFactory.getMarker("SESSION_CREATE");

    @Transactional
    public EventSession createEventSession(EventSession eventSession) {

        logger.info(SESSION_CREATE,"createEventSession method entered");



        //createdby para ver organization

        return eventSessionsRepository.save(eventSession);
    }

    @Transactional
    public EventSession updateEventSession(UUID id, EventSession eventSession) {
        logger.info(SESSION_UPDATE,"updateEventSession method entered");
        if (!id.equals(eventSession.getId())) {
            logger.error(SESSION_UPDATE,"Parameter id and body id do not correspond");
            throw new InvalidEventUpdateException("Parameter id and body id do not correspond");
        }

        EventSession existingEventSession = eventSessionsRepository.findById(id)
                .orElse(null);

        if(existingEventSession==null){
            logger.error(SESSION_UPDATE,"Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        //VERIFICAR SE ALTERA CORRETAMENTE

        return eventSessionsRepository.save(existingEventSession);
    }

    public EventSession getEventSession(UUID eventId) {
        logger.info(SESSION_GET,"getEventSession method entered");

        EventSession eventSession= eventSessionsRepository
                .findById(eventId)
                .orElse(null);

        if(eventSession==null){
            logger.error(SESSION_GET,"Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        return eventSession;
    }

    public void deleteEventSession(UUID id) {
        logger.info(SESSION_DELETE,"deleteEventSession method entered");
        EventSession eventSessionToDelete = eventSessionsRepository.findById(id).orElse(null);

        if (eventSessionToDelete == null) {
            logger.error(SESSION_DELETE,"Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        eventSessionsRepository.delete(eventSessionToDelete);
    }

}
