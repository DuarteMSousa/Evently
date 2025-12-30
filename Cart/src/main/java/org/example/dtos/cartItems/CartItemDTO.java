package org.example.dtos.cartItems;

import java.util.Date;
import java.util.UUID;

public class CartItemDTO {
    private UUID id;

    private UUID productId;

    private int quantity;

    private float unitPrice;

    private Date createdAt;

    private Date updatedAt;
}
