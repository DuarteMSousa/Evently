package org.example.repositories;

import org.example.enums.EventStatus;
import org.example.models.Event;
import org.example.models.EventSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventSessionsRepository extends JpaRepository<EventSession, UUID> {

    @Query(
            "SELECT DISTINCT s " +
                    "FROM EventSession s " +
                    "LEFT JOIN FETCH s.tiers t " +
                    "WHERE s.event.id = :id"
    )
    List<EventSession> findSessionsWithTiersByEventId(@Param("id") UUID eventId);


    @Query(
            "SELECT DISTINCT s " +
                    "FROM EventSession s " +
                    "WHERE s.venueId = :venueId and s.startsAt< :end  and s.endsAt > :start"
    )
    List<EventSession> findSessionsByVenueAndInterval(@Param("venueId") UUID venueId,@Param("start") Instant startsAt, @Param("end") Instant endsAt );
}
