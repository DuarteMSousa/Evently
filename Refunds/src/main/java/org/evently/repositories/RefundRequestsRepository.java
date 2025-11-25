package org.evently.repositories;

import org.evently.models.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRequestsRepository extends JpaRepository<RefundRequest, UUID> {
}
