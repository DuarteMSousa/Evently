package org.evently.controllers;

import org.evently.dtos.RefundRequestMessages.RefundRequestMessageCreateDTO;
import org.evently.dtos.RefundRequestMessages.RefundRequestMessageDTO;
import org.evently.exceptions.InvalidRefundRequestMessageException;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.models.RefundRequestMessage;
import org.evently.services.RefundRequestMessagesService;
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
@RequestMapping("/refunds/messages")
public class RefundRequestMessagesController {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestMessagesController.class);
    private static final Marker MESSAGE_GET = MarkerFactory.getMarker("MESSAGE_GET");
    private static final Marker MESSAGE_SEND = MarkerFactory.getMarker("MESSAGE_SEND");

    @Autowired
    private RefundRequestMessagesService messagesService;

    @GetMapping("/get-message/{id}")
    public ResponseEntity<?> getMessage(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Refund request message found.
         * 404 NOT_FOUND - No message exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */

        logger.info(MESSAGE_GET, "Method getMessage entered for ID: {}", id);
        try {
            RefundRequestMessage message = messagesService.getRefundRequestMessage(id);
            logger.info(MESSAGE_GET, "200 OK returned, message found");
            return ResponseEntity.ok(convertToDTO(message));
        } catch (RefundRequestMessageNotFoundException e) {
            logger.warn(MESSAGE_GET, "RefundRequestMessageNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(MESSAGE_GET, "Exception caught while getting message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/request/{requestId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<RefundRequestMessageDTO>> getMessagesByRequest(
            @PathVariable("requestId") UUID requestId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of messages for the refund request retrieved successfully.
         */

        logger.info(MESSAGE_GET, "Method getMessagesByRequest entered for requestId: {}", requestId);
        RefundRequest request = new RefundRequest();
        request.setId(requestId);

        Page<RefundRequestMessage> messagePage = messagesService.getRefundRequestMessagesByRequest(request, pageNumber, pageSize);
        Page<RefundRequestMessageDTO> dtoPage = messagePage.map(this::convertToDTO);

        logger.info(MESSAGE_GET, "200 OK returned, messages list retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestBody RefundRequestMessageCreateDTO messageDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Message sent successfully.
         * 404 NOT_FOUND - Refund request not found for the provided ID.
         * 400 BAD_REQUEST - Invalid data or system error.
         */

        logger.info(MESSAGE_SEND, "Method sendMessage entered for requestId: {}", messageDTO.getRefundRequestId());
        try {
            RefundRequestMessage message = new RefundRequestMessage();
            message.setUserId(messageDTO.getUser());
            message.setContent(messageDTO.getContent());
            message.setCreatedAt(new Date());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setId(messageDTO.getRefundRequestId());
            message.setRefundRequest(refundRequest);

            RefundRequestMessage saved = messagesService.sendRefundRequestMessage(message);
            logger.info(MESSAGE_SEND, "201 CREATED returned, message sent");
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (RefundRequestNotFoundException e) {
            logger.warn(MESSAGE_SEND, "RefundRequestNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidRefundRequestMessageException e){
            logger.warn(MESSAGE_SEND, "InvalidRefundRequestMessageException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(MESSAGE_SEND, "Exception caught while sending message: {}", e.getMessage());
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