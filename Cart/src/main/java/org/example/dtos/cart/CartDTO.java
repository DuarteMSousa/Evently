package org.example.dtos.cart;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.cartItems.CartItemDTO;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CartDTO {

    private UUID userId;

    private List<CartItemDTO> items;

    private Date createdAt;

    private Date updatedAt;

}
