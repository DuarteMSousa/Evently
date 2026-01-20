package org.evently.listeners;

import org.evently.config.MQConfig;
import org.evently.enums.externalServices.PaymentEventType;
import org.evently.enums.externalServices.PaymentStatus;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.messages.received.PaymentEventMessage;
import org.evently.models.RefundRequest;
import org.evently.services.RefundRequestsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentsListener {

    private static final Logger logger = LoggerFactory.getLogger(RefundRequestsService.class);

    @Autowired
    private RefundRequestsService refundRequestsService;

    @RabbitListener(queues = MQConfig.PAYMENTS_QUEUE)
    public void listener(PaymentEventMessage payment) {

        if (payment.getEventType() == PaymentEventType.REFUND && payment.getStatus() == PaymentStatus.REFUNDED) {
            RefundRequest refundRequest = null;

            try{
                refundRequest = refundRequestsService.getActiveRefundRequestByOrder(payment.getOrderId());
            } catch(RefundRequestNotFoundException e){
                logger.warn("Active refund request not found (orderId={})", payment.getOrderId());
            }

            if (refundRequest != null){
                refundRequestsService.markAsProcessed(refundRequest.getId());
            }
        }
    }
}
