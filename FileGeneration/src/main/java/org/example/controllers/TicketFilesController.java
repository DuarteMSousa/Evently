package org.example.controllers;

import org.example.messages.TicketGeneratedMessage;
import org.example.services.TicketFileGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/fileGeneration/ticket-files")
public class TicketFilesController {

    @Autowired
    private TicketFileGenerationService ticketFileGenerationService;

    /**
     * Generates and saves a ticket PDF file on the server file system.
     * HTTP Status:
     * - 200 OK: ticket file generated and saved successfully
     * - 400 BAD_REQUEST: generic error while generating or saving file (depending on exception handling)
     *
     * @param ticketMessage message containing ticket id and required metadata (at minimum ticket id)
     * @return 200 OK if saved successfully
     */
    @PostMapping("/generate-ticket-file")
    public ResponseEntity<?> saveTicketPdf(@RequestBody TicketGeneratedMessage ticketMessage){
        ticketFileGenerationService.saveTicketFile(ticketMessage);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Retrieves the ticket PDF file for download.
     * HTTP Status:
     * - 200 OK: ticket PDF found and returned
     * - 404 NOT_FOUND: file does not exist (depending on exception handling)
     * - 400 BAD_REQUEST: generic error while reading file (depending on exception handling)
     *
     * @param id ticket identifier (UUID)
     * @return PDF binary content
     */
    @GetMapping("/get-ticket-file/{id}")
    public ResponseEntity<?> getTicketPdf(@PathVariable("id") UUID id){
        byte[] pdf = ticketFileGenerationService.getTicketPdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
