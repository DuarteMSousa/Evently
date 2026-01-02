package org.example.messages.externalServices;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class TicketPdfEventMessage {

    private UUID ticketId;      // ID do ticket / ficheiro
    private UUID userId;        // quem vai receber a notificação
    private UUID orderId;       // opcional (se existir no teu domínio)
    private String eventType;   // "TICKET_PDF_GENERATED" | "TICKET_PDF_FAILED"
    private String fileName;    // ex: "<ticketId>.pdf"
    private String downloadUrl; // ex: "http://filegeneration:8080/ticket-files/get-ticket-file/<ticketId>"
    private String error;       // opcional: mensagem se falhou
}