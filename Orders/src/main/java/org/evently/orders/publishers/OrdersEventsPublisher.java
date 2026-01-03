package org.evently.orders.publishers;

import org.evently.orders.config.MQConfig;
import org.evently.orders.dtos.orderLines.OrderLineDTO;
import org.evently.orders.messages.OrderCreatedMessage;
import org.evently.orders.messages.OrderPayedMessage;
import org.evently.orders.models.Order;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrdersEventsPublisher {
    private final RabbitTemplate rabbitTemplate;

    private ModelMapper modelMapper = new ModelMapper();

    public OrdersEventsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreatedEvent(Order order) {
        OrderCreatedMessage orderCreatedMessage = new OrderCreatedMessage();
        orderCreatedMessage.setId(order.getId());
        orderCreatedMessage.setUserId(order.getUserId());
        orderCreatedMessage.setTotal(order.getTotal());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, orderCreatedMessage);
    }

    public void publishOrderPayedEvent(Order order) {
        OrderPayedMessage orderPayedMessage = new OrderPayedMessage();
        orderPayedMessage.setId(order.getId());
        orderPayedMessage.setUserId(order.getUserId());
        orderPayedMessage.setStatus(order.getStatus());
        orderPayedMessage.setTotal(order.getTotal());

        List<OrderLineDTO> orderLines = new ArrayList<>();
        modelMapper.map(order.getLines(), orderLines);

        orderPayedMessage.setLines(orderLines);

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, orderPayedMessage);
    }
}
