package org.evently.orders.services;

import jakarta.transaction.Transactional;
import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.InvalidOrderUpdateException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.repositories.OrdersRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrdersService {

    private static final Logger logger = LoggerFactory.getLogger(OrdersService.class);

    private static final Marker ORDER_CREATE = MarkerFactory.getMarker("ORDER_CREATE");
    private static final Marker ORDER_GET = MarkerFactory.getMarker("ORDER_GET");
    private static final Marker ORDER_CANCEL = MarkerFactory.getMarker("ORDER_CANCEL");
    private static final Marker ORDER_USE = MarkerFactory.getMarker("ORDER_USE");
    private static final Marker ORDER_VALIDATION = MarkerFactory.getMarker("ORDER_VALIDATION");

    @Autowired
    private OrdersRepository ordersRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    private void validateOrder(Order order) {
        logger.debug(ORDER_VALIDATION, "Validating order payload for user (userId={})", order.getUserId());

        if (order.getUserId() == null) {
            logger.warn(ORDER_VALIDATION, "Missing userId");
            throw new InvalidOrderUpdateException("User ID is required");
        }

        if (order.getTotal() == null || order.getTotal().compareTo(BigDecimal.ZERO) < 0) {
            logger.warn(ORDER_VALIDATION, "Invalid total amount ({})", order.getTotal());
            throw new InvalidOrderUpdateException("Total amount must be greater than or equal to 0");
        }

        if (order.getStatus() == null) {
            logger.warn(ORDER_VALIDATION, "Missing order status");
            throw new InvalidOrderUpdateException("Order status is required");
        }

        if (order.getLines() == null || order.getLines().isEmpty()) {
            logger.warn(ORDER_VALIDATION, "Order has no lines");
            throw new InvalidOrderUpdateException("An order must have at least one line item");
        }

        order.getLines().forEach(line -> {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                logger.warn(ORDER_VALIDATION, "Invalid quantity in order line");
                throw new InvalidOrderUpdateException("Line quantity must be greater than 0");
            }
        });
    }

    public Order getOrder(UUID id) {
        logger.debug(ORDER_GET, "Get order requested (id={})", id);

        return ordersRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(ORDER_GET, "Order not found (id={})", id);
                    return new OrderNotFoundException("Order not found");
                });
    }

    @Transactional
    public Order createOrder(Order order) {
        logger.info(ORDER_CREATE, "Creating order for user (userId={}) with total ({})",
                order.getUserId(), order.getTotal());

        validateOrder(order);

        if (order.getLines() != null) {
            order.getLines().forEach(line -> line.setOrder(order));
        }

        Order savedOrder = ordersRepository.save(order);

        logger.info(ORDER_CREATE, "Order created successfully (id={}, status={})",
                savedOrder.getId(), savedOrder.getStatus());

        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        logger.info(ORDER_CANCEL, "Cancelling order (id={})", id);

        Order order = getOrder(id);

        if (order.getStatus() == OrderStatus.PAYMENT_SUCCESS){
            throw new InvalidOrderUpdateException("Order is already payed successfully");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderUpdateException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        Order cancelledOrder = ordersRepository.save(order);

        logger.info(ORDER_CANCEL, "Order cancelled successfully (id={})", id);

        return cancelledOrder;
    }

    public Page<Order> getOrdersByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        pageSize = Math.min(pageSize, 50);

        logger.debug(ORDER_GET, "Fetching orders for user (userId={}, page={}, size={})",
                userId, pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return ordersRepository.findAllByUserId(userId, pageable);
    }
}