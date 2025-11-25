package org.evently.repositories;

import org.evently.models.RefundDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundDecisionsRepository extends JpaRepository<RefundDecision, UUID> {
}
