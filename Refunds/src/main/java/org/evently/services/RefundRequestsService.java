package org.evently.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.clients.PaymentsClient;
import org.evently.clients.UsersClient;
import org.evently.enums.RefundRequestStatus;
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

import java.util.List;
import java.util.UUID;

@Service
public class RefundRequestsService {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestsService.class);

    private static final Marker REFUND_CREATE = MarkerFactory.getMarker("REFUND_CREATE");
    private static final Marker REFUND_UPDATE = MarkerFactory.getMarker("REFUND_UPDATE");
    private static final Marker REFUND_GET = MarkerFactory.getMarker("REFUND_GET");
    private static final Marker REFUND_VALIDATION = MarkerFactory.getMarker("REFUND_VALIDATION");

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    @Autowired
    private UsersClient usersClient;

    @Autowired
    private PaymentsClient paymentsClient;

    private final ModelMapper modelMapper = new ModelMapper();

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
        logger.info(REFUND_CREATE, "Creating refund request (user={}, payment={})",
                refundRequest.getUserId(), refundRequest.getPaymentId());

        validateRefundRequest(refundRequest);
        validateNoActiveRefund(refundRequest.getPaymentId());
        refundRequest.setStatus(RefundRequestStatus.PENDING);

        try {
            usersClient.getUser(refundRequest.getUserId());
        } catch (FeignException.NotFound e) {
            logger.warn(REFUND_CREATE, "(RefundRequestsService): User not found in Users service");
            throw new UserNotFoundException(
                    "(RefundRequestsService): User not found in Users service");
        } catch (FeignException e) {
            logger.error(REFUND_CREATE, "(RefundRequestsService): Users service error", e);
            throw new ExternalServiceException(
                    "(RefundRequestsService): Users service error");
        }

        try {
            paymentsClient.checkPaymentStatus(refundRequest.getPaymentId());
        } catch (FeignException.NotFound e) {
            logger.warn(REFUND_CREATE, "(RefundRequestsService): Payment not found in Payments service");
            throw new PaymentNotFoundException(
                    "(RefundRequestsService): Payment not found in Payments service");
        } catch (FeignException e) {
            logger.error(REFUND_CREATE, "(RefundRequestsService): Payments service error", e);
            throw new ExternalServiceException(
                    "(RefundRequestsService): Payments service error");
        }

        RefundRequest saved = refundRequestsRepository.save(refundRequest);
        logger.info(REFUND_CREATE, "Refund request created (id={})", saved.getId());
        return saved;
    }

    /**
     * Updates an existing refund request identified by its unique identifier.
     *
     * @param id refund request identifier
     * @param request refund request data to update
     * @return updated refund request
     * @throws InvalidRefundRequestException if the request data is invalid or IDs do not match
     * @throws RefundRequestNotFoundException if the refund request does not exist
     */
    @Transactional
    public RefundRequest updateRefundRequest(UUID id, RefundRequest request) {
        logger.info(REFUND_UPDATE, "Update refund request requested (id={})", id);

        if (request.getId() != null && !id.equals(request.getId())) {
            logger.error(REFUND_UPDATE, "ID mismatch: path={}, body={}", id, request.getId());
            throw new InvalidRefundRequestException("Parameter id and body id do not correspond");
        }

        RefundRequest existing = refundRequestsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(REFUND_UPDATE, "Refund request not found for update (id={})", id);
                    return new RefundRequestNotFoundException("Refund Request not found");
                });

        validateRefundRequest(request);
        modelMapper.map(request, existing);

        RefundRequest updated = refundRequestsRepository.save(existing);
        logger.info(REFUND_UPDATE, "Refund request updated successfully (id={})", updated.getId());
        return updated;
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
            pageNumber = 1;
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

        if (request.getPaymentId() == null) {
            logger.warn(REFUND_VALIDATION, "Missing paymentId");
            throw new InvalidRefundRequestException("Payment ID is required");
        }
        if (request.getUserId() == null) {
            logger.warn(REFUND_VALIDATION, "Missing userId");
            throw new InvalidRefundRequestException("User ID is required");
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
     * Validates if there isn´t already an active or processed refund to the given payment.
     *
     * @param paymentId payment to validate
     * @throws InvalidRefundRequestException if any required field is missing or invalid
     */
    private void validateNoActiveRefund(UUID paymentId) {
        RefundRequestStatus[]  statuses = { RefundRequestStatus.PENDING, RefundRequestStatus.APPROVED,
                RefundRequestStatus.PROCESSED};

        boolean existsActive =
                refundRequestsRepository.existsByPaymentIdAndStatusIn(paymentId, statuses);

        if (existsActive) {
            logger.warn(REFUND_VALIDATION,
                    "Active or processed refund already exists for paymentId={}", paymentId);
            throw new InvalidRefundRequestException(
                    "There is already an active or processed refund for this payment"
            );
        }
    }

}