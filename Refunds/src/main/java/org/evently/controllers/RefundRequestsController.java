package org.evently.controllers;

import org.evently.dtos.RefundRequests.RefundRequestCreateDTO;
import org.evently.dtos.RefundRequests.RefundRequestDTO;
import org.evently.enums.RefundRequestStatus;
import org.evently.exceptions.InvalidRefundRequestUpdateException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.services.RefundRequestsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/refunds")
public class RefundRequestsController {

    @Autowired
    private RefundRequestsService refundRequestsService;

    @GetMapping("/get-refund/{id}")
    public ResponseEntity<?> getRefundRequest(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Refund request found.
         * 404 NOT_FOUND - No refund request exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */
        try {
            RefundRequest refundRequest = refundRequestsService.getRefundRequest(id);
            return ResponseEntity.ok(convertToDTO(refundRequest));
        } catch (RefundRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<RefundRequestDTO>> getRefundRequestsByUser(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of refund requests by user retrieved successfully.
         */
        Page<RefundRequest> refundPage = refundRequestsService.getRefundRequestsByUser(userId, page, size);
        Page<RefundRequestDTO> dtoPage = refundPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-refund")
    public ResponseEntity<?> registerRefund(@RequestBody RefundRequestCreateDTO refundDTO) {
        /*
         * 201 CREATED - Refund request registered successfully.
         * 400 BAD_REQUEST - Invalid data or system error.
         */
        try {
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setPaymentId(refundDTO.getPayment());
            refundRequest.setUserId(refundDTO.getUser());
            refundRequest.setTitle(refundDTO.getTitle());
            refundRequest.setDescription(refundDTO.getDescription());
            refundRequest.setStatus(RefundRequestStatus.PENDING);

            RefundRequest savedRefund = refundRequestsService.createRefundRequest(refundRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedRefund));
        } catch (InvalidRefundRequestUpdateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

//    @PutMapping("/update-refund/{id}")
//    public ResponseEntity<?> updateRefund(@PathVariable("id") UUID id, @RequestBody RefundRequestUpdateDTO refundDTO) {
//        /*
//         * 200 OK - Refund request updated successfully.
//         * 404 NOT_FOUND - Refund request not found for the provided ID.
//         * 400 BAD_REQUEST - Validation error or ID mismatch.
//         */
//        try {
//            RefundRequest updateData = new RefundRequest();
//            updateData.setId(id);
//            updateData.setTitle(refundDTO.getTitle());
//            updateData.setDescription(refundDTO.getDescription());
//            updateData.setStatus(refundDTO.getStatus());
//            updateData.setPaymentId(refundDTO.getPaymentId());
//            updateData.setUserId(refundDTO.getUserId());
//
//            RefundRequest updated = refundRequestsService.updateRefundRequest(id, updateData);
//            return ResponseEntity.ok(convertToDTO(updated));
//        } catch (RefundRequestNotFoundException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        } catch (InvalidRefundRequestUpdateException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }

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