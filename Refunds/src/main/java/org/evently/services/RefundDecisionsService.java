package org.evently.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.clients.UsersClient;
import org.evently.enums.DecisionType;
import org.evently.enums.RefundRequestStatus;
import org.evently.exceptions.*;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.publishers.RefundsEventsPublisher;
import org.evently.repositories.RefundDecisionsRepository;
import org.evently.repositories.RefundRequestsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class RefundDecisionsService {

    private static final Logger logger = LoggerFactory.getLogger(RefundDecisionsService.class);

    private static final Marker DECISION_REGISTER = MarkerFactory.getMarker("DECISION_REGISTER");
    private static final Marker DECISION_GET = MarkerFactory.getMarker("DECISION_GET");
    private static final Marker DECISION_VALIDATION = MarkerFactory.getMarker("DECISION_VALIDATION");

    @Autowired
    private RefundDecisionsRepository refundDecisionsRepository;

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    @Autowired
    private UsersClient usersClient;

    @Autowired
    private RefundsEventsPublisher refundsEventsPublisher;

    /**
     * Retrieves a refund decision by its unique identifier.
     *
     * @param id refund decision identifier
     * @return found refund decision
     * @throws RefundRequestDecisionNotFoundException if the decision does not exist
     */
    public RefundDecision getRefundDecision(UUID id) {
        logger.debug(DECISION_GET, "Get decision requested (id={})", id);
        return refundDecisionsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(DECISION_GET, "Decision not found (id={})", id);
                    return new RefundRequestDecisionNotFoundException("Refund Decision not found");
                });
    }

    /**
     * Retrieves a refund decision by its refund request unique identifier.
     *
     * @param requestId refund request identifier
     * @return found refund decision
     * @throws RefundRequestDecisionNotFoundException if the decision does not exist
     */
    public RefundDecision getRefundDecisionByRequest(UUID requestId) {
        logger.debug(DECISION_GET, "Get decision requested by refund request (requestId={})", requestId);
        return refundDecisionsRepository.findByRefundRequest_Id(requestId)
                .orElseThrow(() -> {
                    logger.warn(DECISION_GET, "Decision not found for the refund request (requestId={})", requestId);
                    return new RefundRequestDecisionNotFoundException("Refund Decision not found");
                });
    }

    /**
     * Registers a new refund decision after validating its data and related entities.
     *
     * @param decision refund decision to be registered
     * @return persisted refund decision
     * @throws InvalidRefundRequestDecisionException if the decision data is invalid
     * @throws UserNotFoundException if the deciding user does not exist
     * @throws RefundRequestNotFoundException if the associated refund request does not exist
     * @throws ExternalServiceException if the Users service is unavailable or returns an error
     */
    @Transactional
    public RefundDecision registerRefundDecision(RefundDecision decision) {
        logger.info(DECISION_REGISTER, "Registering decision ({}) for request (requestId={})",
                decision.getDecisionType(),
                decision.getRefundRequest() != null ? decision.getRefundRequest().getId() : "NULL");

        try {
            usersClient.getUser(decision.getDecidedBy());
        } catch (FeignException.NotFound e) {
            logger.warn(DECISION_REGISTER, "(RefundDecisionsService): User not found in Users service");
            throw new UserNotFoundException(
                    "(RefundDecisionsService): User not found in Users service");
        } catch (FeignException e) {
            logger.error(DECISION_REGISTER, "(RefundDecisionsService): Users service error", e);
            throw new ExternalServiceException(
                    "(RefundDecisionsService): Users service error");
        }

        RefundRequest refundRequest = refundRequestsRepository.findById(decision.getRefundRequest().getId())
                .orElseThrow(() -> {
                    logger.warn(DECISION_REGISTER, "Parent refund request not found (id={})",
                            decision.getRefundRequest().getId());
                    return new RefundRequestNotFoundException("Refund Request not found");
                });

        if (refundRequest.getUserId().equals(decision.getDecidedBy())) {
            throw new InvalidRefundRequestDecisionException("The user who made the refund request cannot give a decision about it");
        }

        decision.setRefundRequest(refundRequest);

        validateDecision(decision);

        RefundDecision saved = refundDecisionsRepository.save(decision);
        logger.info(DECISION_REGISTER, "Decision registered successfully (id={}, type={})",
                saved.getId(), saved.getDecisionType());

        /// changing refund request status based on the decision
        if (decision.getDecisionType() == DecisionType.APPROVE) {
            saved.getRefundRequest().setStatus(RefundRequestStatus.APPROVED);
        } else {
            saved.getRefundRequest().setStatus(RefundRequestStatus.REJECTED);
        }

        /// setting the decision date
        saved.getRefundRequest().setDecisionAt(new Date());

        refundRequestsRepository.save(saved.getRefundRequest());

        /// sending a message with the decision
        refundsEventsPublisher.publishRefundRequestDecisionRegisteredEvent(saved);

        return saved;
    }

    /***
     * Validates all required fields of a refund decision before registration.
     *
     * @param decision refund decision to validate
     * @throws InvalidRefundRequestDecisionException if any required field is missing or invalid
     */
    private void validateDecision(RefundDecision decision) {
        logger.debug(DECISION_VALIDATION, "Validating decision payload");
        if (decision.getDecidedBy() == null) {
            throw new InvalidRefundRequestDecisionException("DecidedBy is required");
        }
        if (decision.getDecisionType() == null) {
            throw new InvalidRefundRequestDecisionException("Decision Type (APPROVE/REJECT) is required");
        }
        if (decision.getRefundRequest() == null || decision.getRefundRequest().getId() == null) {
            throw new InvalidRefundRequestDecisionException("Decision must be linked to a Refund Request");
        }
        if (refundDecisionsRepository.existsByRefundRequest_Id(decision.getRefundRequest().getId())) {
            throw new InvalidRefundRequestDecisionException("This refund request already has a decision");
        }
        if (!decision.getRefundRequest().getStatus().equals(RefundRequestStatus.PENDING)) {
            throw new InvalidRefundRequestDecisionException("Only PENDING refund requests can be decided");
        }
    }

}