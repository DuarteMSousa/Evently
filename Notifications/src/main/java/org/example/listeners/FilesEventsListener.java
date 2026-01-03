package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.TicketFileGeneratedMessage;
import org.example.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FilesEventsListener {

    private final NotificationsService notificationsService;

    public FilesEventsListener(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_FILES_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleFileGenerated(TicketFileGeneratedMessage msg) {

        String fileName = msg.getId() + ".pdf";

        String downloadUrl = "http://localhost:8083/ticket-files/get-ticket-file/" + msg.getId();

        notificationsService.notifyPdfGenerated(
                msg.getUserId(),
                msg.getOrderId(),
                fileName,
                downloadUrl
        );
    }
}