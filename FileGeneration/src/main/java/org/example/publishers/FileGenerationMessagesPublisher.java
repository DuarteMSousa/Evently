package org.example.publishers;

import org.example.config.MQConfig;
import org.example.messages.TicketFileGeneratedMessage;
import org.example.messages.TicketGeneratedMessage;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileGenerationMessagesPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ModelMapper modelMapper = new ModelMapper();


    public void publishTicketFileGeneratedMessage(TicketGeneratedMessage ticket) {
        TicketFileGeneratedMessage message = modelMapper.map(ticket, TicketFileGeneratedMessage.class);

        rabbitTemplate.convertAndSend(MQConfig.FILES_EXCHANGE,MQConfig.FILES_ROUTING_KEY+".generated",message);
    }

}
