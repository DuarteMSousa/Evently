package org.evently.orders.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.orders.clients.EventsClient;
import org.evently.orders.config.MQConfig;
import org.evently.orders.dtos.externalServices.SessionTierDTO;
import org.evently.orders.dtos.orderLines.OrderLineDTO;
import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.ExternalServiceException;
import org.evently.orders.exceptions.InvalidOrderException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.exceptions.externalServices.ProductNotFoundException;
import org.evently.orders.messages.OrderCreatedMessage;
import org.evently.orders.messages.OrderPayedMessage;
import org.evently.orders.models.Order;
import org.evently.orders.models.OrderLine;
import org.evently.orders.publishers.OrdersEventsPublisher;
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

import java.util.Date;
import java.util.UUID;

@Service
public class OrdersService {

    private static final Logger logger = LoggerFactory.getLogger(OrdersService.class);

    private static final Marker ORDER_CREATE = MarkerFactory.getMarker("ORDER_CREATE");
    private static final Marker ORDER_GET = MarkerFactory.getMarker("ORDER_GET");
    private static final Marker ORDER_CANCEL = MarkerFactory.getMarker("ORDER_CANCEL");
    private static final Marker ORDER_PAYMENT = MarkerFactory.getMarker("ORDER_PAYMENT");
    private static final Marker ORDER_VALIDATION = MarkerFactory.getMarker("ORDER_VALIDATION");

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private EventsClient  eventsClient;

    private OrdersEventsPublisher  ordersEventsPublisher;

    private ModelMapper modelMapper = new ModelMapper();

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id order identifier
     * @return found order
     * @throws OrderNotFoundException if the order does not exist
     */
    public Order getOrder(UUID id) {
        logger.debug(ORDER_GET, "Get order requested (id={})", id);

        return ordersRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(ORDER_GET, "Order not found (id={})", id);
                    return new OrderNotFoundException("Order not found");
                });
    }

    /**
     * Creates a new order after validating all required fields and associations.
     *
     * @param order order to be created (must contain userId and order lines)
     * @return persisted order with calculated total and prices
     *
     * @throws ProductNotFoundException if a referenced product/session tier does not exist
     * @throws ExternalServiceException if an error occurs while communicating with EventsService
     * @throws InvalidOrderException if order validation fails
     */
    @Transactional
    public Order createOrder(Order order) {
        logger.info(ORDER_CREATE, "Creating order for user (userId={}) with total ({})",
                order.getUserId(), order.getTotal());

        float orderTotal = 0;

        /// Recreating order lines based in actual products prices
        for (OrderLine line : order.getLines()) {

            SessionTierDTO tier;

            try {
                tier = eventsClient.getSessionTier(line.getId().getProductId()).getBody();
            } catch (FeignException.NotFound e) {
                String errorBody = e.contentUTF8();
                logger.error(ORDER_CREATE, "(OrdersService): Tier not found in Events service: {}", errorBody);
                throw new ProductNotFoundException("(OrdersService): Tier not found in Events service");
            } catch (FeignException e) {
                String errorBody = e.contentUTF8();
                logger.error(ORDER_CREATE, "(OrdersService): Events service error: {}", errorBody);
                throw new ExternalServiceException("(OrdersService): Events service error");
            }

            if (tier == null) {
                logger.error(ORDER_CREATE, "Product not found");
                throw new ProductNotFoundException("Product not found");
            }

            line.setUnitPrice(tier.getPrice());
            orderTotal += line.getQuantity() * line.getUnitPrice();
        }

        order.setStatus(OrderStatus.CREATED);
        order.setTotal(orderTotal);
        order.setCreatedAt(new Date());

        validateOrder(order);

        Order savedOrder = ordersRepository.save(order);

        logger.info(ORDER_CREATE, "Order created successfully (id={}, status={})",
                savedOrder.getId(), savedOrder.getStatus());

        ordersEventsPublisher.publishOrderCreatedEvent(savedOrder);

        return savedOrder;
    }

    /**
     * Marks an existing order as successfully paid.
     *
     * @param id order identifier
     * @return updated order marked as paid
     * @throws OrderNotFoundException      if the order does not exist
     * @throws InvalidOrderException if the order is not in a payable state
     */
    @Transactional
    public Order markAsPaid(UUID id) {
        logger.info(ORDER_PAYMENT, "Marking order as paid (id={})", id);

        Order order = getOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderException("Cannot pay order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAYMENT_SUCCESS);
        order.setPaidAt(new Date());

        Order updatedOrder = ordersRepository.save(order);
        logger.info(ORDER_PAYMENT, "Order marked as paid successfully (id={})", id);

        ordersEventsPublisher.publishOrderPayedEvent(updatedOrder);

        return updatedOrder;
    }

    /**
     * Marks an existing order payment as failed.
     *
     * @param id order identifier
     * @return updated order marked as payment failed
     * @throws OrderNotFoundException      if the order does not exist
     * @throws InvalidOrderException if the order is not in a payable state
     */
    @Transactional
    public Order markAsPaymentFailed(UUID id) {
        logger.info(ORDER_PAYMENT, "Marking order as failed (id={})", id);

        Order order = getOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderException("Cannot fail payment for order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);

        Order updatedOrder = ordersRepository.save(order);
        logger.info(ORDER_PAYMENT, "Order marked as payment failed (id={})", id);
        return updatedOrder;
    }

    /**
     * Cancels an existing order, as long as it has not been successfully paid or already cancelled.
     *
     * @param id order identifier
     * @return cancelled order
     * @throws OrderNotFoundException      if the order does not exist
     * @throws InvalidOrderException if the order is already paid or cancelled
     */
    @Transactional
    public Order cancelOrder(UUID id) {
        logger.info(ORDER_CANCEL, "Cancelling order (id={})", id);

        Order order = getOrder(id);

        if (order.getStatus() == OrderStatus.PAYMENT_SUCCESS) {
            throw new InvalidOrderException("Order is already payed successfully");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaidAt(new Date());

        Order cancelledOrder = ordersRepository.save(order);

        logger.info(ORDER_CANCEL, "Order cancelled successfully (id={})", id);

        return cancelledOrder;
    }

    /**
     * Retrieves a paginated list of orders associated with a user.
     *
     * @param userId     user identifier
     * @param pageNumber page number (0-based)
     * @param pageSize   page size (maximum 50)
     * @return page of user orders
     */
    public Page<Order> getOrdersByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        pageSize = Math.min(pageSize, 50);

        logger.debug(ORDER_GET, "Fetching orders for user (userId={}, page={}, size={})",
                userId, pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return ordersRepository.findAllByUserId(userId, pageable);
    }

    /**
     * Validates all required order fields before persisting the order.
     *
     * @param order order to validate
     * @throws InvalidOrderException if any required field or constraint is invalid
     */
    private void validateOrder(Order order) {
        logger.debug(ORDER_VALIDATION, "Validating order payload for user (userId={})", order.getUserId());

        if (order.getUserId() == null) {
            logger.warn(ORDER_VALIDATION, "Missing userId");
            throw new InvalidOrderException("User ID is required");
        }

        if (order.getTotal() <= 0) {
            logger.warn(ORDER_VALIDATION, "Invalid total amount ({})", order.getTotal());
            throw new InvalidOrderException("Total amount must be greater than or equal to 0");
        }

        if (order.getStatus() == null) {
            logger.warn(ORDER_VALIDATION, "Missing order status");
            throw new InvalidOrderException("Order status is required");
        }

        if (order.getLines() == null || order.getLines().isEmpty()) {
            logger.warn(ORDER_VALIDATION, "Order has no lines");
            throw new InvalidOrderException("An order must have at least one line item");
        }

        order.getLines().forEach(line -> {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                logger.warn(ORDER_VALIDATION, "Invalid quantity in order line");
                throw new InvalidOrderException("Line quantity must be greater than 0");
            }
        });
    }
}