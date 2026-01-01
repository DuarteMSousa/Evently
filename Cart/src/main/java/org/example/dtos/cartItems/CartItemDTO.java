package org.example.dtos.cartItems;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class CartItemDTO {
    private UUID id;

    private UUID productId;

    private int quantity;

    private float unitPrice;

    private Date createdAt;

    private Date updatedAt;
}
