package org.example.publishers;

import org.example.config.MQConfig;
import org.example.messages.EventTicketStockGeneratedMessage;
import org.example.messages.EventTicketStockGenerationFailedMessage;
import org.example.messages.TicketReservationConfirmedMessage;
import org.example.models.TicketReservation;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventTicketManagementMessagesPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ModelMapper modelMapper = new ModelMapper();


    public void publishTicketReservationConfirmedMessage(TicketReservation reservation) {
        TicketReservationConfirmedMessage message = modelMapper.map(reservation, TicketReservationConfirmedMessage.class) ;

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY+".reservation.confirmed", message);
    }

    public void publishEventTicketStockGeneratedMessage(UUID eventId) {
        EventTicketStockGeneratedMessage message = new EventTicketStockGeneratedMessage() ;
        message.setEventId(eventId);

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY+".stock.generated", message);
    }

    public void publishEventTicketStockGenerationFailedMessage(UUID eventId) {
        EventTicketStockGenerationFailedMessage message = new EventTicketStockGenerationFailedMessage() ;
        message.setEventId(eventId);

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY+".stock.generation.failed", message);
    }
}
