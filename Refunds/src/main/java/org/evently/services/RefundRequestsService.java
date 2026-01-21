package org.evently.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.clients.OrdersClient;
import org.evently.clients.PaymentsClient;
import org.evently.clients.UsersClient;
import org.evently.dtos.externalServices.orders.OrderDTO;
import org.evently.dtos.externalServices.payments.PaymentDTO;
import org.evently.enums.RefundRequestStatus;
import org.evently.enums.externalServices.orders.OrderStatus;
import org.evently.enums.externalServices.payments.PaymentStatus;
import org.evently.exceptions.ExternalServiceException;
import org.evently.exceptions.InvalidRefundRequestException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.exceptions.externalServices.PaymentNotFoundException;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundRequestsRepository;
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
public class RefundRequestsService {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestsService.class);

    private static final Marker REFUND_CREATE = MarkerFactory.getMarker("REFUND_CREATE");
    private static final Marker REFUND_PROCESS = MarkerFactory.getMarker("REFUND_PROCESS");
    private static final Marker REFUND_GET = MarkerFactory.getMarker("REFUND_GET");
    private static final Marker REFUND_VALIDATION = MarkerFactory.getMarker("REFUND_VALIDATION");

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    @Autowired
    private PaymentsClient paymentsClient;

    @Autowired
    private OrdersClient ordersClient;

    /**
     * Retrieves a refund request by its unique identifier.
     *
     * @param id refund request identifier
     * @return found refund request
     * @throws RefundRequestNotFoundException if the refund request does not exist
     */
    public RefundRequest getRefundRequest(UUID id) {
        logger.debug(REFUND_GET, "Get refund request requested (id={})", id);
        return refundRequestsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(REFUND_GET, "Refund request not found (id={})", id);
                    return new RefundRequestNotFoundException("Refund Request not found");
                });
    }

    /**
     * Creates a new refund request after validating its data and
     * verifying related external entities.
     *
     * @param refundRequest refund request to be created
     * @return persisted refund request
     * @throws InvalidRefundRequestException if the refund request data is invalid
     * @throws UserNotFoundException if the requesting user does not exist
     * @throws PaymentNotFoundException if the associated payment does not exist
     * @throws ExternalServiceException if the Users or Payments service is unavailable or returns an error
     */
    @Transactional
    public RefundRequest createRefundRequest(RefundRequest refundRequest) {
        logger.info(REFUND_CREATE, "Creating refund request (order={})", refundRequest.getOrderId());

        validateRefundRequest(refundRequest);
        validateNoActiveRefund(refundRequest.getOrderId());
        refundRequest.setStatus(RefundRequestStatus.PENDING);

        OrderDTO orderDTO;

        try{
            orderDTO = ordersClient.getOrder(refundRequest.getOrderId()).getBody();
        } catch (FeignException.NotFound e) {
            logger.warn(REFUND_CREATE, "(RefundRequestsService): Order not found in Orders service");
            throw new PaymentNotFoundException(
                    "(RefundRequestsService): Order not found in Orders service");
        } catch (FeignException e) {
            logger.error(REFUND_CREATE, "(RefundRequestsService): Orders service error", e);
            throw new ExternalServiceException(
                    "(RefundRequestsService): Orders service error");
        }

        if (orderDTO != null){
            if (orderDTO.getStatus() != OrderStatus.PAYMENT_SUCCESS){
                throw new InvalidRefundRequestException("A refund request cannot be created for an order that is not paid yet");
            }
        }

        PaymentDTO paymentDTO;

        try {
            paymentDTO = paymentsClient.getPaymentByOrder(refundRequest.getOrderId()).getBody();
        } catch (FeignException.NotFound e) {
            logger.warn(REFUND_CREATE, "(RefundRequestsService): Payment not found in Payments service");
            throw new PaymentNotFoundException(
                    "(RefundRequestsService): Payment not found in Payments service");
        } catch (FeignException e) {
            logger.error(REFUND_CREATE, "(RefundRequestsService): Payments service error", e);
            throw new ExternalServiceException(
                    "(RefundRequestsService): Payments service error");
        }

        if (paymentDTO != null){
            refundRequest.setPaymentId(paymentDTO.getId());
            refundRequest.setUserId(paymentDTO.getUserId());

            if (paymentDTO.getStatus() != PaymentStatus.CAPTURED) {
                throw new InvalidRefundRequestException("Payment is not processed yet");
            }
        }

        RefundRequest saved = refundRequestsRepository.save(refundRequest);
        logger.info(REFUND_CREATE, "Refund request created (id={})", saved.getId());
        return saved;
    }

    /**
     * Marks an existing refund as processed.
     *
     * @param id refund identifier
     * @return updated refund marked as processed
     * @throws RefundRequestNotFoundException if the refund does not exist
     * @throws InvalidRefundRequestException if the refund is not in a processable state
     */
    @Transactional
    public RefundRequest markAsProcessed(UUID id) {
        logger.info(REFUND_PROCESS, "Marking refund as processed (id={})", id);

        RefundRequest refundRequest = getRefundRequest(id);

        if (refundRequest.getStatus() != RefundRequestStatus.APPROVED) {
            throw new InvalidRefundRequestException("Cannot process refund in status: " + refundRequest.getStatus());
        }

        refundRequest.setStatus(RefundRequestStatus.PROCESSED);
        refundRequest.setProcessedAt(new Date());

        RefundRequest updatedRefund = refundRequestsRepository.save(refundRequest);
        logger.info(REFUND_PROCESS, "Refund marked as processed successfully (id={})", id);
        return updatedRefund;
    }

    /**
     * Retrieves a paginated list of refund requests associated with a user.
     *
     * @param userId user identifier
     * @param pageNumber page number (1-based)
     * @param pageSize page size
     * @return page of refund requests for the given user
     */
    public Page<RefundRequest> getRefundRequestsByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 0;
        }

        logger.debug(REFUND_GET, "Fetching refund requests for user (userId={}, page={})", userId, pageNumber);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundRequestsRepository.findAllByUserId(userId, pageable);
    }

    /**
     * Validates all required fields of a refund request before creation or update.
     *
     * @param request refund request to validate
     * @throws InvalidRefundRequestException if any required field is missing or invalid
     */
    private void validateRefundRequest(RefundRequest request) {
        logger.debug(REFUND_VALIDATION, "Validating refund request payload");

        if (request.getOrderId() == null) {
            logger.warn(REFUND_VALIDATION, "Missing orderId");
            throw new InvalidRefundRequestException("Order ID is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            logger.warn(REFUND_VALIDATION, "Title is empty");
            throw new InvalidRefundRequestException("Title is required");
        }
        if(request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            logger.warn(REFUND_VALIDATION, "Description is empty");
            throw new InvalidRefundRequestException("Description is required");
        }
    }

    /**
     * Retrieves the active (PENDING) refund request for a given order.
     *
     * @param orderId order identifier
     * @return active refund request
     * @throws RefundRequestNotFoundException if no active refund request exists
     */
    public RefundRequest getActiveRefundRequestByOrder(UUID orderId) {
        logger.debug(REFUND_GET, "Get active refund request requested (orderId={})", orderId);

        RefundRequest activeRefundRequest =
                refundRequestsRepository.findOneByOrderIdAndStatus(
                        orderId,
                        RefundRequestStatus.APPROVED
                );

        if (activeRefundRequest == null) {
            logger.warn(REFUND_GET,
                    "Active refund request not found (orderId={})", orderId);
            throw new RefundRequestNotFoundException(
                    "No active refund request found for this payment"
            );
        }

        return activeRefundRequest;
    }

    /**
     * Validates if there isn´t already an active or processed refund to the given order.
     *
     * @param orderId order to validate
     * @throws InvalidRefundRequestException if any required field is missing or invalid
     */
    private void validateNoActiveRefund(UUID orderId) {
        RefundRequestStatus[]  statuses = { RefundRequestStatus.PENDING, RefundRequestStatus.APPROVED,
                RefundRequestStatus.PROCESSED};

        boolean existsActive =
                refundRequestsRepository.existsByOrderIdAndStatusIn(orderId, statuses);

        if (existsActive) {
            logger.warn(REFUND_VALIDATION,
                    "Active or processed refund already exists for orderId={}", orderId);
            throw new InvalidRefundRequestException(
                    "There is already an active or processed refund for this order"
            );
        }
    }

}