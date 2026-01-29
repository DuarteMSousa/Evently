package org.example.clients;

import org.example.dtos.externalServices.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "file-generation", path = "/fileGeneration/ticket-files")
public interface FileClient {

    @GetMapping("/get-ticket-file/{id}")
    public ResponseEntity<byte[]> getTicketPdf(@PathVariable("id") UUID id);
}