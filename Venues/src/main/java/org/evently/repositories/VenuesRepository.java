package org.evently.repositories;

import org.evently.models.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VenuesRepository extends JpaRepository<Venue, UUID>, JpaSpecificationExecutor<Venue> {

    boolean existsByName(String name);

}