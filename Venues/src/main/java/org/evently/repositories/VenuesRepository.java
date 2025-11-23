package org.evently.repositories;

import org.evently.models.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenuesRepository extends JpaRepository<Venue, UUID> {

    boolean existsByName(String name);
}