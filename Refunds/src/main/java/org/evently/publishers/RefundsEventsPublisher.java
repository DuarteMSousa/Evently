package org.evently.publishers;

import org.evently.config.MQConfig;
import org.evently.messages.RefundRequestDecisionRegisteredMessage;
import org.evently.messages.RefundRequestMessageSentMessage;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequestMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RefundsEventsPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RefundsEventsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRefundRequestDecisionRegisteredEvent(RefundDecision decision) {
        RefundRequestDecisionRegisteredMessage refundRequestDecisionRegistered = new RefundRequestDecisionRegisteredMessage();
        refundRequestDecisionRegistered.setUserToRefundId(decision.getRefundRequest().getUserId());
        refundRequestDecisionRegistered.setPaymentId(decision.getRefundRequest().getPaymentId());
        refundRequestDecisionRegistered.setDecisionType(decision.getDecisionType());
        refundRequestDecisionRegistered.setDescription(decision.getDescription());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, refundRequestDecisionRegistered);
    }

    public void publishRefundRequestMessageSentEvent(RefundRequestMessage message) {
        RefundRequestMessageSentMessage refundRequestMessageSent = new RefundRequestMessageSentMessage();
        refundRequestMessageSent.setUserId(message.getUserId());
        refundRequestMessageSent.setContent(message.getContent());
        refundRequestMessageSent.setRefundRequestId(message.getRefundRequest().getId());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, refundRequestMessageSent);
    }

}
