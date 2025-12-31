package org.evently.controllers;

import org.evently.dtos.RefundRequests.RefundRequestCreateDTO;
import org.evently.dtos.RefundRequests.RefundRequestDTO;
import org.evently.enums.RefundRequestStatus;
import org.evently.exceptions.InvalidRefundRequestException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.services.RefundRequestsService;
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
@RequestMapping("/refunds")
public class RefundRequestsController {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestsController.class);
    private static final Marker REFUND_GET = MarkerFactory.getMarker("REFUND_GET");
    private static final Marker REFUND_CREATE = MarkerFactory.getMarker("REFUND_CREATE");

    @Autowired
    private RefundRequestsService refundRequestsService;

    @GetMapping("/get-refund/{id}")
    public ResponseEntity<?> getRefundRequest(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Refund request found.
         * 404 NOT_FOUND - No refund request exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */

        logger.info(REFUND_GET, "Method getRefundRequest entered for ID: {}", id);
        try {
            RefundRequest refundRequest = refundRequestsService.getRefundRequest(id);
            logger.info(REFUND_GET, "200 OK returned, refund request found");
            return ResponseEntity.ok(convertToDTO(refundRequest));
        } catch (RefundRequestNotFoundException e) {
            logger.warn(REFUND_GET, "RefundRequestNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(REFUND_GET, "Exception caught while getting refund request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-by-user/{userId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<RefundRequestDTO>> getRefundRequestsByUser(
            @PathVariable("userId") UUID userId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of refund requests by user retrieved successfully.
         */

        logger.info(REFUND_GET, "Method getRefundRequestsByUser entered for user: {}", userId);
        Page<RefundRequest> refundPage = refundRequestsService.getRefundRequestsByUser(userId, pageNumber, pageSize);
        Page<RefundRequestDTO> dtoPage = refundPage.map(this::convertToDTO);

        logger.info(REFUND_GET, "200 OK returned, paginated list retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-refund")
    public ResponseEntity<?> registerRefund(@RequestBody RefundRequestCreateDTO refundDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Refund request registered successfully.
         * 400 BAD_REQUEST - Invalid data or system error.
         */
        logger.info(REFUND_CREATE, "Method registerRefund entered for user: {}", refundDTO.getUser());
        try {
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setPaymentId(refundDTO.getPayment());
            refundRequest.setUserId(refundDTO.getUser());
            refundRequest.setTitle(refundDTO.getTitle());
            refundRequest.setDescription(refundDTO.getDescription());
            refundRequest.setStatus(RefundRequestStatus.PENDING);
            refundRequest.setCreatedAt(new Date());

            RefundRequest savedRefund = refundRequestsService.createRefundRequest(refundRequest);
            logger.info(REFUND_CREATE, "201 CREATED returned, refund request registered");
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedRefund));
        }catch (InvalidRefundRequestException e){
            logger.warn(REFUND_CREATE, "InvalidRefundRequestException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(REFUND_CREATE, "Exception caught while registering refund: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private RefundRequestDTO convertToDTO(RefundRequest refund) {
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setId(refund.getId());
        dto.setPaymentId(refund.getPaymentId());
        dto.setUserId(refund.getUserId());
        dto.setTitle(refund.getTitle());
        dto.setDescription(refund.getDescription());
        dto.setStatus(refund.getStatus());
        dto.setCreatedAt(refund.getCreatedAt());
        dto.setDecisionAt(refund.getDecisionAt());
        dto.setProcessedAt(refund.getProcessedAt());
        return dto;
    }
}