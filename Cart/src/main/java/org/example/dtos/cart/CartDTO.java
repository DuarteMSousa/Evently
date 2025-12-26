package org.example.dtos.cart;

import org.example.dtos.cartItems.CartItemDTO;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CartDTO {

    private UUID userId;

    private List<CartItemDTO> items;

    private Date createdAt;

    private Date updatedAt;
}
