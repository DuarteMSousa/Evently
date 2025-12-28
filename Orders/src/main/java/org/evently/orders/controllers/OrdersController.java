package org.evently.orders.controllers;

import org.evently.orders.dtos.orderLines.OrderLineDTO;
import org.evently.orders.dtos.orders.OrderCreateDTO;
import org.evently.orders.dtos.orders.OrderDTO;
import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.InvalidOrderUpdateException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.models.OrderLine;
import org.evently.orders.models.OrderLineId;
import org.evently.orders.services.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @GetMapping("/get-order/{id}")
    public ResponseEntity<?> getOrder(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Order found.
         * 404 NOT_FOUND - Order does not exist.
         * 400 BAD_REQUEST - System error.
         */
        try {
            Order order = ordersService.getOrder(id);
            return ResponseEntity.ok(convertToDTO(order));
        } catch (OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByUser(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of orders retrieved.
         */
        Page<Order> orderPage = ordersService.getOrdersByUser(userId, page, size);
        Page<OrderDTO> dtoPage = orderPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-order")
    public ResponseEntity<?> registerOrder(@RequestBody OrderCreateDTO orderDTO) {
        /*
         * 201 CREATED - Order created.
         * 400 BAD_REQUEST - Validation error.
         */
        try {
            Order orderRequest = new Order();
            orderRequest.setUserId(orderDTO.getUserId());
            orderRequest.setStatus(OrderStatus.CREATED);
            orderRequest.setTotal(orderDTO.getTotal());

            if (orderDTO.getLines() != null) {
                List<OrderLine> lines = orderDTO.getLines().stream().map(lineDTO -> {
                    OrderLine line = new OrderLine();
                    line.setId(new OrderLineId(null, lineDTO.getProductId()));
                    line.setQuantity(lineDTO.getQuantity());
                    line.setUnitPrice(lineDTO.getUnitPrice());
                    return line;
                }).collect(Collectors.toList());
                orderRequest.setLines(lines);
            }

            Order savedOrder = ordersService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedOrder));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/mark-order-payment-success/{id}")
    public ResponseEntity<?> markAsPaid(@PathVariable UUID id) {
        /*
         * 200 OK - Payment marked as successful.
         * 404 NOT_FOUND - No order exists with the provided ID.
         * 400 BAD_REQUEST - Order is not in a state that allows payment.
         */
        try {
            Order updatedOrder = ordersService.markAsPaid(id);
            return ResponseEntity.ok(convertToDTO(updatedOrder));
        } catch (OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @PatchMapping("/mark-order-payment-failed/{id}")
    public ResponseEntity<?> markAsFailed(@PathVariable UUID id) {
        /*
         * 200 OK - Order marked as payment failed.
         * 404 NOT_FOUND - No order exists with the provided ID.
         * 400 BAD_REQUEST - Order is not in a state that allows failure.
         */
        try {
            Order updatedOrder = ordersService.markAsPaymentFailed(id);
            return ResponseEntity.ok(convertToDTO(updatedOrder));
        } catch (OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @PatchMapping("/cancel-order/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Order cancelled successfully.
         * 404 NOT_FOUND - No order exists with the provided ID.
         * 400 BAD_REQUEST - Order is already used or already cancelled.
         */
        try {
            Order cancelledOrder = ordersService.cancelOrder(id);
            return ResponseEntity.ok(convertToDTO(cancelledOrder));
        } catch (OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setTotal(order.getTotal());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPaidAt(order.getPaidAt());
        dto.setCanceledAt(order.getCanceledAt());

        if (order.getLines() != null) {
            List<OrderLineDTO> lineDTOs = order.getLines().stream().map(line -> {
                OrderLineDTO lDto = new OrderLineDTO();
                lDto.setProductId(line.getId().getProductId());
                lDto.setQuantity(line.getQuantity());
                lDto.setUnitPrice(line.getUnitPrice());
                return lDto;
            }).collect(Collectors.toList());
            dto.setLines(lineDTOs);
        }

        return dto;
    }
}