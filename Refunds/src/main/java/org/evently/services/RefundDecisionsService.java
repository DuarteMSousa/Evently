package org.evently.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.clients.UsersClient;
import org.evently.exceptions.ExternalServiceException;
import org.evently.exceptions.InvalidRefundRequestUpdateException;
import org.evently.exceptions.RefundDecisionNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundDecisionsRepository;
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

    public RefundDecision getRefundDecision(UUID id) {
        logger.debug(DECISION_GET, "Get decision requested (id={})", id);
        return refundDecisionsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(DECISION_GET, "Decision not found (id={})", id);
                    return new RefundDecisionNotFoundException("Refund Decision not found");
                });
    }

    @Transactional
    public RefundDecision registerRefundDecision(RefundDecision decision) {
        logger.info(DECISION_REGISTER, "Registering decision ({}) for request (requestId={})",
                decision.getDecisionType(),
                decision.getRefundRequest() != null ? decision.getRefundRequest().getId() : "NULL");

        validateDecision(decision);

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

        if (!refundRequestsRepository.existsById(decision.getRefundRequest().getId())) {
            logger.warn(DECISION_REGISTER, "Parent refund request not found (id={})", decision.getRefundRequest().getId());
            throw new RefundRequestNotFoundException("Refund Request not found");
        }

        RefundDecision saved = refundDecisionsRepository.save(decision);
        logger.info(DECISION_REGISTER, "Decision registered successfully (id={}, type={})",
                saved.getId(), saved.getDecisionType());
        return saved;
    }

    public Page<RefundDecision> getRefundDecisionsByRequest(RefundRequest refundRequest, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(DECISION_GET, "Fetching decisions for request (requestId={})", refundRequest.getId());

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundDecisionsRepository.findAllByRefundRequest(refundRequest, pageable);
    }

    private void validateDecision(RefundDecision decision) {
        logger.debug(DECISION_VALIDATION, "Validating decision payload");
        if (decision.getDecidedBy() == null) {
            throw new InvalidRefundRequestUpdateException("DecidedBy is required");
        }
        if (decision.getDecisionType() == null) {
            throw new InvalidRefundRequestUpdateException("Decision Type (APPROVE/REJECT) is required");
        }
        if (decision.getRefundRequest() == null || decision.getRefundRequest().getId() == null) {
            throw new InvalidRefundRequestUpdateException("Decision must be linked to a Refund Request");
        }
    }
}