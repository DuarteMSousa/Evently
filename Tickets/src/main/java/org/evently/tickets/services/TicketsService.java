package org.evently.tickets.services;

import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.repositories.TicketsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class TicketsService {

    @Autowired
    private TicketsRepository ticketsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public Ticket getTicket(UUID id) {
        return ticketsRepository
                .findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
    }

    @Transactional
    public Ticket registerTicket(Ticket ticket) {
        return ticketsRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(UUID id, Ticket ticket) {
        if (!id.equals(ticket.getId())) {
            throw new InvalidTicketUpdateException("Parameter id and body id do not correspond");
        }

        Ticket existingTicket = ticketsRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        modelMapper.map(ticket, existingTicket);

        return ticketsRepository.save(existingTicket);
    }

    public Page<Ticket> getTicketsByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        if(pageSize > 50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return ticketsRepository.FindAllByUserId(userId, pageable);
    }
}
