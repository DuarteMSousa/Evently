package org.evently.tickets.repositories;

import org.evently.tickets.models.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketsRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> FindAllByUserId(UUID userId, PageRequest pageRequest);

}
