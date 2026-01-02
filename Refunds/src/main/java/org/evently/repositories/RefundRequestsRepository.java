package org.evently.repositories;

import org.evently.enums.RefundRequestStatus;
import org.evently.models.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RefundRequestsRepository extends JpaRepository<RefundRequest, UUID> {

    Page<RefundRequest> findAllByUserId(UUID userId, PageRequest pageRequest);

    RefundRequest findOneByPaymentIdAndStatus(UUID paymentId, RefundRequestStatus status);

    boolean existsByPaymentIdAndStatusIn(
            UUID paymentId,
            RefundRequestStatus[] statuses
    );

}
