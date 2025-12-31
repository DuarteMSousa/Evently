package org.example.clients;


import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.events.EventDTO;
import org.example.dtos.externalServices.orders.OrderCreateDTO;
import org.example.dtos.externalServices.orders.OrderDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "orders", path = "/orders")
public interface OrdersClient {

    @GetMapping("/register-order")
    ResponseEntity<OrderDTO> registerOrder(@RequestBody OrderCreateDTO orderDTO);

}
