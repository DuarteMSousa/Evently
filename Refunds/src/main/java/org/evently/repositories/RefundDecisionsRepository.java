package org.evently.repositories;

import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundDecisionsRepository extends JpaRepository<RefundDecision, UUID> {

    Page<RefundDecision> findAllByRefundRequest(RefundRequest refundRequest, PageRequest pageRequest);

}
