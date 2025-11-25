package org.example.services;

import org.evently.users.Utils.PasswordUtils;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.example.enums.EventStatus;
import org.example.exceptions.EventAlreadyCanceledException;
import org.example.exceptions.EventAlreadyExistsException;
import org.example.exceptions.EventNotFoundException;
import org.example.exceptions.InvalidEventUpdateException;
import org.example.models.Event;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
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

        return eventsRepository.save(event);
    }

    @Transactional
    public Event updateEvent(UUID id, Event event) {
        if (!id.equals(event.getId())) {
            throw new InvalidEventUpdateException("Parameter id and body id do not correspond");
        }

        Event existingEvent = eventsRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

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
        Event eventToCancel = eventsRepository.findById(userId)
                .orElseThrow(() -> new EventNotFoundException(""));

        if (!eventToCancel.getStatus().equals(EventStatus.CANCELED)) {
            throw new EventAlreadyCanceledException("Event already cancelled");
        }

        eventToCancel.setStatus(EventStatus.CANCELED);
        return eventsRepository.save(eventToCancel);
    }

    public String loginUser(UUID userId, String password) {
        if (!usersRepository.existsById(userId)) {
            throw new LoginFailedException("");
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new LoginFailedException(""));

        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            throw new LoginFailedException("");
        }

        return "";
    }

    public Page<Event> getEventPage(Integer pageNumber, Integer pageSize) {
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return eventsRepository.findAll(pageable);
    }
}
