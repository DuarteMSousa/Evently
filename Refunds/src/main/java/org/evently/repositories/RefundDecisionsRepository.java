package org.evently.repositories;

import org.evently.models.RefundDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefundDecisionsRepository extends JpaRepository<RefundDecision, UUID> {

    boolean existsByRefundRequest_Id(UUID refundRequestId);

    Optional<RefundDecision> findByRefundRequest_Id(UUID refundRequestId);

}
