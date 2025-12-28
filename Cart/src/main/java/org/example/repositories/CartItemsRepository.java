package org.example.repositories;

import org.example.models.Cart;
import org.example.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartItemsRepository extends JpaRepository<CartItem, UUID> {

}
