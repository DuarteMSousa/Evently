package org.example.repositories;

import org.example.models.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentEventsRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByPayment_Id(UUID paymentId);

}