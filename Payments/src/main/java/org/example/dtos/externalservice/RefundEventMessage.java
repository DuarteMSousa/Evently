package org.example.dtos.externalservice;

import java.util.UUID;

public class RefundEventMessage {

    private UUID refundId;
    private UUID paymentId;
    private float amount;
    private String status;
    private String eventType;

    public RefundEventMessage() {}

    public UUID getRefundId() { return refundId; }
    public void setRefundId(UUID refundId) { this.refundId = refundId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
