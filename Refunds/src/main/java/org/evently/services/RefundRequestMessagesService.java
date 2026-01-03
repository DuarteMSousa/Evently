package org.evently.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.clients.UsersClient;
import org.evently.exceptions.*;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundRequestMessage;
import org.evently.publishers.RefundsEventsPublisher;
import org.evently.repositories.RefundRequestMessagesRepository;
import org.evently.repositories.RefundRequestsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefundRequestMessagesService {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestMessagesService.class);

    private static final Marker MESSAGE_SEND = MarkerFactory.getMarker("MSG_SEND");
    private static final Marker MESSAGE_GET = MarkerFactory.getMarker("MSG_GET");
    private static final Marker MESSAGE_VALIDATION = MarkerFactory.getMarker("MSG_VALIDATION");

    @Autowired
    private RefundRequestMessagesRepository refundRequestMessagesRepository;

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    @Autowired
    private UsersClient usersClient;

    @Autowired
    private RefundsEventsPublisher refundsEventsPublisher;

    /**
     * Retrieves a refund request message by its unique identifier.
     *
     * @param id refund request message identifier
     * @return found refund request message
     * @throws RefundRequestMessageNotFoundException if the message does not exist
     */
    public RefundRequestMessage getRefundRequestMessage(UUID id) {
        logger.debug(MESSAGE_GET, "Get message requested (id={})", id);
        return refundRequestMessagesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(MESSAGE_GET, "Message not found (id={})", id);
                    return new RefundRequestMessageNotFoundException("Refund Request Message not found");
                });
    }

    /**
     * Sends (registers) a new refund request message after validating its data
     * and verifying related entities.
     *
     * @param message refund request message to be sent
     * @return persisted refund request message
     * @throws InvalidRefundRequestMessageException if the message data is invalid
     * @throws UserNotFoundException if the message author does not exist
     * @throws RefundRequestNotFoundException if the associated refund request does not exist
     * @throws ExternalServiceException if the Users service is unavailable or returns an error
     */
    @Transactional
    public RefundRequestMessage sendRefundRequestMessage(RefundRequestMessage message) {
        logger.info(MESSAGE_SEND, "Sending message for refund request (requestId={})",
                message.getRefundRequest() != null ? message.getRefundRequest().getId() : "NULL");

        validateMessage(message);

        try {
            usersClient.getUser(message.getUserId());
        } catch (FeignException.NotFound e) {
            logger.warn(MESSAGE_SEND, "(RefundRequestMessagesService): User not found in Users service");
            throw new UserNotFoundException(
                    "(RefundRequestMessagesService): User not found in Users service");
        } catch (FeignException e) {
            logger.error(MESSAGE_SEND, "(RefundRequestMessagesService): Users service error", e);
            throw new ExternalServiceException(
                    "(RefundRequestMessagesService): Users service error");
        }

        if (!refundRequestsRepository.existsById(message.getRefundRequest().getId())) {
            logger.warn(MESSAGE_SEND, "Parent refund request not found (id={})", message.getRefundRequest().getId());
            throw new RefundRequestNotFoundException("Refund Request not found");
        }

        RefundRequestMessage saved = refundRequestMessagesRepository.save(message);
        logger.info(MESSAGE_SEND, "Message sent successfully (id={})", saved.getId());

        refundsEventsPublisher.publishRefundRequestMessageSentEvent(saved);

        return saved;
    }

    /**
     * Retrieves a paginated list of messages associated with a refund request.
     *
     * @param refundRequest refund request entity
     * @param pageNumber page number (1-based)
     * @param pageSize page size
     * @return page of refund request messages for the given request
     */
    public Page<RefundRequestMessage> getRefundRequestMessagesByRequest(org.evently.models.RefundRequest refundRequest, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 0;
        }

        logger.debug(MESSAGE_GET, "Fetching messages for request (requestId={})", refundRequest.getId());

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundRequestMessagesRepository.findAllByRefundRequest(refundRequest, pageable);
    }

    /**
     * Validates all required fields of a refund request message before sending.
     *
     * @param message refund request message to validate
     * @throws InvalidRefundRequestMessageException if any required field is missing or invalid
     */
    private void validateMessage(RefundRequestMessage message) {
        logger.debug(MESSAGE_VALIDATION, "Validating refund request message payload");

        if (message.getUserId() == null) {
            logger.warn(MESSAGE_VALIDATION, "Missing userId");
            throw new InvalidRefundRequestMessageException("User ID is required");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            logger.warn(MESSAGE_VALIDATION, "Message is empty");
            throw new InvalidRefundRequestMessageException("Message content cannot be empty");
        }
        if (message.getRefundRequest() == null || message.getRefundRequest().getId() == null) {
            logger.warn(MESSAGE_VALIDATION, "Message is not linked to a refund request");
            throw new InvalidRefundRequestMessageException("Message must be linked to a Refund Request");
        }
    }
}