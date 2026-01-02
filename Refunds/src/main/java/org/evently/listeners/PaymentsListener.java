package org.evently.listeners;

import org.evently.config.MQConfig;
import org.evently.enums.externalServices.PaymentEventType;
import org.evently.enums.externalServices.PaymentStatus;
import org.evently.messages.received.PaymentEventMessage;
import org.evently.models.RefundRequest;
import org.evently.services.RefundRequestsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentsListener {

    @Autowired
    private RefundRequestsService refundRequestsService;

    @RabbitListener(queues = MQConfig.PAYMENTS_QUEUE)
    public void listener(PaymentEventMessage payment) {

        if (payment.getEventType() == PaymentEventType.REFUND && payment.getStatus() == PaymentStatus.REFUNDED) {
            RefundRequest refundRequest = refundRequestsService.getActiveRefundRequestByPayment(payment.getPaymentId());

            if (refundRequest != null){
                refundRequestsService.markAsProcessed(refundRequest.getId());
            }
        }
    }
}
