package org.example.controllers;

import org.example.messages.TicketMessage;
import org.example.services.TicketFileGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ticket-files")
public class TicketFilesController {

    @Autowired
    private TicketFileGenerationService ticketFileGenerationService;

    @PostMapping("/generate-ticket-file")
    public ResponseEntity<?> saveTicketPdf(@RequestBody TicketMessage ticketMessage){
        ticketFileGenerationService.saveTicketFile(ticketMessage);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/get-ticket-file/{id}")
    public ResponseEntity<?> getTicketPdf(@PathVariable("id") UUID id){
        byte[] pdf = ticketFileGenerationService.getTicketPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
