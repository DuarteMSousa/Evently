package org.example.repositories;

import org.example.enums.EventStatus;
import org.example.models.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EventsRepository extends JpaRepository<Event, UUID> {

    boolean existsByName(String name);

    Page<Event> findAllByStatus(EventStatus status, PageRequest pageable);

    @Query(
            "SELECT DISTINCT e " +
                    "FROM Event e " +
                    "LEFT JOIN FETCH e.sessions s " +
                    "LEFT JOIN FETCH s.tiers " +
                    "WHERE e.id = :id"
    )
    Optional<Event> findByIdWithSessionsAndTiers(@Param("id") UUID id);


}
