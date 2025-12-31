package org.example.models;

import java.util.UUID;

public class PaymentEventMessage {
    private String type;
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private String status;
    private String provider;
    private String providerRef;
}
