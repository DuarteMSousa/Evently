package org.evently.services;

import jakarta.transaction.Transactional;
import org.evently.exceptions.InvalidRefundRequestUpdateException;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequestMessage;
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

    private void validateMessage(RefundRequestMessage message) {
        logger.debug(MESSAGE_VALIDATION, "Validating refund request message payload");

        if (message.getUserId() == null) {
            logger.warn(MESSAGE_VALIDATION, "Missing userId");
            throw new InvalidRefundRequestUpdateException("User ID is required");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            logger.warn(MESSAGE_VALIDATION, "Message is empty");
            throw new InvalidRefundRequestUpdateException("Message content cannot be empty");
        }
        if (message.getRefundRequest() == null || message.getRefundRequest().getId() == null) {
            logger.warn(MESSAGE_VALIDATION, "Message is not linked to a refund request");
            throw new InvalidRefundRequestUpdateException("Message must be linked to a Refund Request");
        }
    }

    public RefundRequestMessage getRefundRequestMessage(UUID id) {
        logger.debug(MESSAGE_GET, "Get message requested (id={})", id);
        return refundRequestMessagesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(MESSAGE_GET, "Message not found (id={})", id);
                    return new RefundRequestMessageNotFoundException("Refund Request Message not found");
                });
    }

    @Transactional
    public RefundRequestMessage sendRefundRequestMessage(RefundRequestMessage message) {
        logger.info(MESSAGE_SEND, "Sending message for refund request (requestId={})",
                message.getRefundRequest() != null ? message.getRefundRequest().getId() : "NULL");

        validateMessage(message);

        if (!refundRequestsRepository.existsById(message.getRefundRequest().getId())) {
            logger.warn(MESSAGE_SEND, "Parent refund request not found (id={})", message.getRefundRequest().getId());
            throw new RefundRequestNotFoundException("Refund Request not found");
        }

        RefundRequestMessage saved = refundRequestMessagesRepository.save(message);
        logger.info(MESSAGE_SEND, "Message sent successfully (id={})", saved.getId());
        return saved;
    }

    public Page<RefundRequestMessage> getRefundRequestMessagesByRequest(org.evently.models.RefundRequest refundRequest, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(MESSAGE_GET, "Fetching messages for request (requestId={})", refundRequest.getId());

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundRequestMessagesRepository.findAllByRefundRequest(refundRequest, pageable);
    }
}