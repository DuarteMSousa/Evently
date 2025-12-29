package org.example.repositories;

import org.example.models.EventSession;
import org.example.models.SessionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionTiersRepository extends JpaRepository<SessionTier, UUID> {
    boolean existsByEventSessionAndZoneId(EventSession eventSession, UUID zoneId);
}
