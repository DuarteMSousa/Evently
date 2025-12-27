package org.evently.controllers;

import org.evently.dtos.RefundRequestMessages.RefundRequestMessageCreateDTO;
import org.evently.dtos.RefundRequestMessages.RefundRequestMessageDTO;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.models.RefundRequestMessage;
import org.evently.services.RefundRequestMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/refunds/messages")
public class RefundRequestMessagesController {

    @Autowired
    private RefundRequestMessagesService messagesService;

    @GetMapping("/get-message/{id}")
    public ResponseEntity<?> getMessage(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Refund request message found.
         * 404 NOT_FOUND - No message exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */
        try {
            RefundRequestMessage message = messagesService.getRefundRequestMessage(id);
            return ResponseEntity.ok(convertToDTO(message));
        } catch (RefundRequestMessageNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<Page<RefundRequestMessageDTO>> getMessagesByRequest(
            @PathVariable("requestId") UUID requestId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of messages for the refund request retrieved successfully.
         */
        RefundRequest request = new RefundRequest();
        request.setId(requestId);

        Page<RefundRequestMessage> messagePage = messagesService.getRefundRequestMessagesByRequest(request, page, size);
        Page<RefundRequestMessageDTO> dtoPage = messagePage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestBody RefundRequestMessageCreateDTO messageDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Message sent successfully.
         * 404 NOT_FOUND - Refund request not found for the provided ID.
         * 400 BAD_REQUEST - Invalid data or system error.
         */
        try {
            RefundRequestMessage message = new RefundRequestMessage();
            message.setUserId(messageDTO.getUser());
            message.setContent(messageDTO.getContent());
            message.setCreatedAt(new Date());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setId(messageDTO.getRefundRequestId());
            message.setRefundRequest(refundRequest);

            RefundRequestMessage saved = messagesService.sendRefundRequestMessage(message);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (RefundRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private RefundRequestMessageDTO convertToDTO(RefundRequestMessage message) {
        RefundRequestMessageDTO dto = new RefundRequestMessageDTO();
        dto.setId(message.getId());
        dto.setUserId(message.getUserId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        if (message.getRefundRequest() != null) {
            dto.setRefundRequestId(message.getRefundRequest().getId());
        }
        return dto;
    }
}