package org.evently.clients;

import org.evently.dtos.externalServices.orders.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "orders", path = "/orders")
public interface OrdersClient {

    @GetMapping("/get-order/{id}")
    ResponseEntity<OrderDTO> getOrder(@PathVariable("id") UUID id);

}
