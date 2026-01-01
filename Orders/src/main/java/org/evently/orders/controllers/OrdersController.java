package org.evently.orders.controllers;

import org.evently.orders.dtos.orderLines.OrderLineDTO;
import org.evently.orders.dtos.orders.OrderCreateDTO;
import org.evently.orders.dtos.orders.OrderDTO;
import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.ExternalServiceException;
import org.evently.orders.exceptions.InvalidOrderUpdateException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.exceptions.externalServices.ProductNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.models.OrderLine;
import org.evently.orders.models.OrderLineId;
import org.evently.orders.services.OrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);

    private static final Marker ORDER_GET = MarkerFactory.getMarker("ORDER_GET");
    private static final Marker ORDER_CREATE = MarkerFactory.getMarker("ORDER_CREATE");
    private static final Marker ORDER_PAYMENT = MarkerFactory.getMarker("ORDER_PAYMENT");
    private static final Marker ORDER_CANCEL = MarkerFactory.getMarker("ORDER_CANCEL");

    @Autowired
    private OrdersService ordersService;

    @GetMapping("/get-order/{id}")
    public ResponseEntity<?> getOrder(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Order found.
         * 404 NOT_FOUND - Order does not exist.
         * 400 BAD_REQUEST - System error.
         */

        logger.info(ORDER_GET, "Method getOrder entered (id={})", id);
        try {
            Order order = ordersService.getOrder(id);
            logger.info(ORDER_GET, "200 OK returned, order found");
            return ResponseEntity.ok(convertToDTO(order));
        } catch (OrderNotFoundException e) {
            logger.error(ORDER_GET, "OrderNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(ORDER_GET, "Exception caught while getting order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByUser(
            @PathVariable("userId") UUID userId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of orders retrieved.
         */

        logger.info(ORDER_GET, "Method getOrdersByUser entered (userId={})", userId);

        Page<Order> orderPage = ordersService.getOrdersByUser(userId, pageNumber, pageSize);
        Page<OrderDTO> dtoPage = orderPage.map(this::convertToDTO);

        logger.info(ORDER_GET, "200 OK returned, orders page retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-order")
    public ResponseEntity<?> registerOrder(@RequestBody OrderCreateDTO orderDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Order created.
         * 400 BAD_REQUEST - Validation error.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */

        logger.info(ORDER_CREATE, "Method registerOrder entered for userId={}", orderDTO.getUserId());
        try {
            Order orderRequest = new Order();
            orderRequest.setUserId(orderDTO.getUserId());
            orderRequest.setStatus(OrderStatus.CREATED);
            orderRequest.setTotal(0);

            if (orderDTO.getLines() != null) {
                List<OrderLine> lines = orderDTO.getLines().stream().map(lineDTO -> {
                    OrderLine line = new OrderLine();
                    line.setId(new OrderLineId(null, lineDTO.getProductId()));
                    line.setQuantity(lineDTO.getQuantity());
                    line.setUnitPrice(0);
                    return line;
                }).collect(Collectors.toList());
                orderRequest.setLines(lines);
            }

            Order savedOrder = ordersService.createOrder(orderRequest);
            logger.info(ORDER_CREATE, "201 CREATED returned, order registered (id={})", savedOrder.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedOrder));
        } catch (InvalidOrderUpdateException e) {
            logger.warn(ORDER_CREATE, "Invalid order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(ORDER_CREATE, "Exception caught while registering order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PatchMapping("/mark-order-payment-success/{id}")
    public ResponseEntity<?> markAsPaid(@PathVariable UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Payment marked as successful.
         * 404 NOT_FOUND - No order exists with the provided ID.
         * 400 BAD_REQUEST - Order is not in a state that allows payment.
         */

        logger.info(ORDER_PAYMENT, "Method markAsPaid entered (id={})", id);
        try {
            Order updatedOrder = ordersService.markAsPaid(id);
            logger.info(ORDER_PAYMENT, "200 OK returned, order marked as paid");
            return ResponseEntity.ok(convertToDTO(updatedOrder));
        } catch (OrderNotFoundException e) {
            logger.error(ORDER_PAYMENT, "OrderNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            logger.error(ORDER_PAYMENT, "InvalidOrderUpdateException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(ORDER_PAYMENT, "Unexpected exception caught in markAsPaid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @PatchMapping("/mark-order-payment-failed/{id}")
    public ResponseEntity<?> markAsFailed(@PathVariable UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Order marked as payment failed.
         * 404 NOT_FOUND - No order exists with the provided ID.
         * 400 BAD_REQUEST - Order is not in a state that allows failure.
         */

        logger.info(ORDER_PAYMENT, "Method markAsFailed entered (id={})", id);
        try {
            Order updatedOrder = ordersService.markAsPaymentFailed(id);
            logger.info(ORDER_PAYMENT, "200 OK returned, order marked as failed");
            return ResponseEntity.ok(convertToDTO(updatedOrder));
        } catch (OrderNotFoundException e) {
            logger.error(ORDER_PAYMENT, "OrderNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            logger.error(ORDER_PAYMENT, "InvalidOrderUpdateException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(ORDER_PAYMENT, "Unexpected exception caught in markAsFailed: {}", e.getMessage());
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

        logger.info(ORDER_CANCEL, "Method cancelOrder entered (id={})", id);
        try {
            Order cancelledOrder = ordersService.cancelOrder(id);
            logger.info(ORDER_CANCEL, "200 OK returned, order cancelled");
            return ResponseEntity.ok(convertToDTO(cancelledOrder));
        } catch (OrderNotFoundException e) {
            logger.error(ORDER_CANCEL, "OrderNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidOrderUpdateException e) {
            logger.error(ORDER_CANCEL, "InvalidOrderUpdateException caught: {}", e.getMessage());
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