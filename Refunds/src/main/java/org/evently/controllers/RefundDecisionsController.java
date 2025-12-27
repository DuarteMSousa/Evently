package org.evently.controllers;

import org.evently.dtos.RefundDecisions.RefundDecisionCreateDTO;
import org.evently.dtos.RefundDecisions.RefundDecisionDTO;
import org.evently.exceptions.RefundDecisionNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.services.RefundDecisionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/refunds/decisions")
public class RefundDecisionsController {

    @Autowired
    private RefundDecisionsService refundDecisionsService;

    @GetMapping("/get-decision/{id}")
    public ResponseEntity<?> getDecision(@PathVariable("id") UUID id) {
        try {
            RefundDecision decision = refundDecisionsService.getRefundDecision(id);
            return ResponseEntity.ok(convertToDTO(decision));
        } catch (RefundDecisionNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<Page<RefundDecisionDTO>> getDecisionsByRequest(
            @PathVariable("requestId") UUID requestId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {

        RefundRequest request = new RefundRequest();
        request.setId(requestId);

        Page<RefundDecision> decisionPage = refundDecisionsService.getRefundDecisionsByRequest(request, page, size);
        Page<RefundDecisionDTO> dtoPage = decisionPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-decision")
    public ResponseEntity<?> registerDecision(@RequestBody RefundDecisionCreateDTO decisionDTO) {
        try {
            RefundDecision decision = new RefundDecision();
            decision.setDecidedBy(decisionDTO.getDecidedBy());
            decision.setDecisionType(decisionDTO.getDecisionType());
            decision.setDescription(decisionDTO.getDescription());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setId(decisionDTO.getRefundRequestId());
            decision.setRefundRequest(refundRequest);

            RefundDecision saved = refundDecisionsService.registerRefundDecision(decision);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (RefundRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
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