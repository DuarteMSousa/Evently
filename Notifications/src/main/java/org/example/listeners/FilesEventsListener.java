package org.example.listeners;

import org.example.config.RabbitMQConfig;
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
    public void handleFileEvent(PdfGeneratedEventMessage msg) {

        if ("PDF_GENERATED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyPdfGenerated(
                    msg.getUserId(),
                    msg.getOrderId(),
                    msg.getFileName(),
                    msg.getDownloadUrl()
            );
        }

        if ("PDF_FAILED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyPdfFailed(msg.getUserId(), msg.getOrderId(), msg.getFileName());
        }
    }
}