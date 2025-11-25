package org.example.repositories;

import org.example.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationsRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);
}