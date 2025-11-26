package org.evently.orders.repositories;

import org.evently.orders.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdersRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByUserId(UUID userId, PageRequest pageRequest);
}
