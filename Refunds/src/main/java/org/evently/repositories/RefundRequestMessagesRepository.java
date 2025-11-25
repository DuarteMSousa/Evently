package org.evently.repositories;

import org.evently.models.RefundRequestMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRequestMessagesRepository extends JpaRepository<RefundRequestMessage, UUID> {
}
