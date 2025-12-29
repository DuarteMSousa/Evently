package org.example.repositories;

import org.example.enums.EventStatus;
import org.example.models.Event;
import org.example.models.EventSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventSessionsRepository extends JpaRepository<EventSession, UUID> {
}
