package org.evently.controllers;

import org.evently.dtos.RefundDecisions.RefundDecisionCreateDTO;
import org.evently.dtos.RefundDecisions.RefundDecisionDTO;
import org.evently.exceptions.InvalidRefundRequestDecisionException;
import org.evently.exceptions.RefundRequestDecisionNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.services.RefundDecisionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/refunds/decisions")
public class RefundDecisionsController {

    private static final Logger logger = LoggerFactory.getLogger(RefundDecisionsController.class);
    private static final Marker DECISION_GET = MarkerFactory.getMarker("DECISION_GET");
    private static final Marker DECISION_REGISTER = MarkerFactory.getMarker("DECISION_REGISTER");

    @Autowired
    private RefundDecisionsService refundDecisionsService;

    @GetMapping("/get-decision/{id}")
    public ResponseEntity<?> getDecision(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Refund decision found.
         * 404 NOT_FOUND - No decision exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */

        logger.info(DECISION_GET, "Method getDecision entered for ID: {}", id);
        try {
            RefundDecision decision = refundDecisionsService.getRefundDecision(id);
            logger.info(DECISION_GET, "200 OK returned, decision found");
            return ResponseEntity.ok(convertToDTO(decision));
        } catch (RefundRequestDecisionNotFoundException e) {
            logger.warn(DECISION_GET, "RefundRequestDecisionNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(DECISION_GET, "Exception caught while getting decision: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/register-decision")
    public ResponseEntity<?> registerDecision(@RequestBody RefundDecisionCreateDTO decisionDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Refund decision registered successfully.
         * 404 NOT_FOUND - Refund request not found for the provided ID.
         * 400 BAD_REQUEST - Invalid data or system error.
         */

        logger.info(DECISION_REGISTER, "Method registerDecision entered for requestId: {}", decisionDTO.getRefundRequestId());
        try {
            RefundDecision decision = new RefundDecision();
            decision.setDecidedBy(decisionDTO.getDecidedBy());
            decision.setDecisionType(decisionDTO.getDecisionType());
            decision.setDescription(decisionDTO.getDescription());
            decision.setCreatedAt(new Date());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setId(decisionDTO.getRefundRequestId());
            decision.setRefundRequest(refundRequest);

            RefundDecision saved = refundDecisionsService.registerRefundDecision(decision);
            logger.info(DECISION_REGISTER, "201 CREATED returned, decision registered");
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (RefundRequestNotFoundException e) {
            logger.warn(DECISION_REGISTER, "RefundRequestNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (InvalidRefundRequestDecisionException e){
            logger.warn(DECISION_REGISTER, "InvalidRefundRequestDecisionException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(DECISION_REGISTER, "Exception caught while registering decision: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private RefundDecisionDTO convertToDTO(RefundDecision decision) {
        RefundDecisionDTO dto = new RefundDecisionDTO();
        dto.setId(decision.getId());
        dto.setDecidedBy(decision.getDecidedBy());
        dto.setDecisionType(decision.getDecisionType());
        dto.setDescription(decision.getDescription());
        dto.setCreatedAt(decision.getCreatedAt());
        if (decision.getRefundRequest() != null) {
            dto.setRefundRequestId(decision.getRefundRequest().getId());
        }
        return dto;
    }
}