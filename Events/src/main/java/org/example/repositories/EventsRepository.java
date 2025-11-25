package org.example.repositories;

import org.example.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventsRepository extends JpaRepository<Event, UUID> {

    boolean existsByName(String name);
}
