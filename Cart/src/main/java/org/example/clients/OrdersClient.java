package org.example.clients;

import org.example.dtos.externalServices.orders.OrderCreateDTO;
import org.example.dtos.externalServices.orders.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "orders", path = "/orders")
public interface OrdersClient {

    @GetMapping("/register-order")
    ResponseEntity<OrderDTO> registerOrder(@RequestBody OrderCreateDTO orderDTO);

}
