package org.example.publishers;

import org.example.config.MQConfig;
import org.example.messages.TicketReservationConfirmedMessage;
import org.example.messages.TicketStockGeneratedMessage;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicketManagementMessagesPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ModelMapper modelMapper = new ModelMapper();


    public void publishTicketReservationConfirmedMessage(TicketReservation reservation) {
        TicketReservationConfirmedMessage message = modelMapper.map(reservation, TicketReservationConfirmedMessage.class) ;

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY+".reservation_confirmed", message);
    }

    public void publishTicketStockGeneratedMessage(TicketStock stock) {
        TicketStockGeneratedMessage message = new TicketStockGeneratedMessage() ;
        message.setEventId(stock.getId().getEventId());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY+".stock_generated", message);
    }
}
