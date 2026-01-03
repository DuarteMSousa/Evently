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
        RefundRequestDecisionRegisteredMessage msg = new RefundRequestDecisionRegisteredMessage();
        msg.setUserToRefundId(decision.getRefundRequest().getUserId());
        msg.setPaymentId(decision.getRefundRequest().getPaymentId());
        msg.setDecisionType(decision.getDecisionType());
        msg.setDescription(decision.getDescription());

        rabbitTemplate.convertAndSend(
                MQConfig.EXCHANGE,
                MQConfig.RK_REFUND_DECISION_REGISTERED,
                msg
        );
    }

    public void publishRefundRequestMessageSentEvent(RefundRequestMessage message) {
        RefundRequestMessageSentMessage msg = new RefundRequestMessageSentMessage();
        msg.setUserId(message.getUserId());
        msg.setContent(message.getContent());
        msg.setRefundRequestId(message.getRefundRequest().getId());

        rabbitTemplate.convertAndSend(
                MQConfig.EXCHANGE,
                MQConfig.RK_REFUND_REQUEST_MESSAGE_SENT,
                msg
        );
    }
}
