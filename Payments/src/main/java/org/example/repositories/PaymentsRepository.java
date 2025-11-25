package org.example.repositories;

import org.example.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentsRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserId(UUID userId);

    Optional<Payment> findByProviderRef(String providerRef);

}