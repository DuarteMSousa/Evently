package org.example.publishers;

import org.example.config.MQConfig;
import org.example.messages.EventPublishedMessage;
import org.example.models.Event;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventMessagesPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ModelMapper modelMapper = new ModelMapper();

    public void publishEventPublishedMessage(Event event) {
        EventPublishedMessage message = modelMapper.map(event, EventPublishedMessage.class);

        rabbitTemplate.convertAndSend(MQConfig.EVENTS_EXCHANGE,MQConfig.EVENTS_ROUTING_KEY+".published",message);
    }

}
