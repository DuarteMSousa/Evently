package org.evently.repositories;

import org.evently.enums.RefundRequestStatus;
import org.evently.models.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRequestsRepository extends JpaRepository<RefundRequest, UUID> {

    Page<RefundRequest> findAllByUserId(UUID userId, PageRequest pageRequest);

    RefundRequest findOneByOrderIdAndStatus(UUID orderId, RefundRequestStatus status);

    boolean existsByOrderIdAndStatusIn(
            UUID orderId,
            RefundRequestStatus[] statuses
    );

}
