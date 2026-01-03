package org.example.listeners;

import lombok.var;
import org.example.clients.FileClient;
import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.TicketFileGeneratedMessage;
import org.example.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FilesEventsListener {

    private final NotificationsService notificationsService;
    private final FileClient fileClient;

    public FilesEventsListener(NotificationsService notificationsService, FileClient fileClient) {
        this.notificationsService = notificationsService;
        this.fileClient = fileClient;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIF_FILES_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void handleFileGenerated(TicketFileGeneratedMessage msg) {

        var resp = fileClient.getTicketPdf(msg.getId());

        if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Could not download pdf for fileId=" + msg.getId());
        }

        byte[] pdfBytes = resp.getBody();
        String fileName = msg.getId() + ".pdf";

        notificationsService.notifyPdfGeneratedWithAttachment(
                msg.getUserId(),
                msg.getOrderId(),
                fileName,
                pdfBytes
        );
    }
}
