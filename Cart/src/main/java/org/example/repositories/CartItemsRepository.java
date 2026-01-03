package org.example.repositories;

import org.example.models.Cart;
import org.example.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemsRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> getCartItemsByCart(Cart cart);

    Optional<CartItem> findByCartAndProductId(Cart cart, UUID productId);

    CartItem getCartItemByProductIdAndCart(UUID productId, Cart cart);
}
