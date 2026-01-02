package org.evently.publishers;

import org.evently.config.MQConfig;
import org.evently.messages.RefundRequestDecisionRegistered;
import org.evently.messages.RefundRequestMessageSent;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequestMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RefundsEventsPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RefundsEventsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRefundRequestDecisionRegisteredEvent(RefundDecision decision) {
        RefundRequestDecisionRegistered refundRequestDecisionRegistered = new RefundRequestDecisionRegistered();
        refundRequestDecisionRegistered.setRefundRequestId(decision.getRefundRequest().getId());
        refundRequestDecisionRegistered.setDecisionType(decision.getDecisionType());
        refundRequestDecisionRegistered.setDescription(decision.getDescription());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, refundRequestDecisionRegistered);
    }

    public void publishRefundRequestMessageSentEvent(RefundRequestMessage message) {
        RefundRequestMessageSent refundRequestMessageSent = new RefundRequestMessageSent();
        refundRequestMessageSent.setUserId(message.getUserId());
        refundRequestMessageSent.setContent(message.getContent());
        refundRequestMessageSent.setRefundRequestId(message.getRefundRequest().getId());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, refundRequestMessageSent);
    }

}
