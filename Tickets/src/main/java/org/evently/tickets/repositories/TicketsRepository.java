package org.evently.tickets.repositories;

import org.evently.tickets.models.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketsRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findAllByUserId(UUID userId, Pageable pageable);

}
