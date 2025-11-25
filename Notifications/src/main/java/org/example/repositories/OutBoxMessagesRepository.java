package org.example.repositories;

import org.example.models.OutBoxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutBoxMessagesRepository extends JpaRepository<OutBoxMessage, UUID> {
}