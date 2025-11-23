package org.evently.repositories;

import org.evently.models.VenueZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenueZoneRepository extends JpaRepository<VenueZone, UUID> {

    List<VenueZone> findByVenueId(UUID venueId);
}