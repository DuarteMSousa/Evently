package org.example.repositories;

import org.example.models.TicketReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketReservationsRepository extends JpaRepository<TicketReservation, UUID> {

    boolean existsByEventId(UUID eventId);

    boolean existsBySessionId(UUID sessionId);

    boolean existsByTierId(UUID tierId);
}
