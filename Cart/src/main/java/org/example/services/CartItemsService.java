package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.*;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartItemsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartItemsService {

    private Logger logger = LoggerFactory.getLogger(CartItemsService.class);

    private Marker addMarker = MarkerFactory.getMarker("AddCartItem");

    private Marker updateMarker = MarkerFactory.getMarker("UpdateCartItem");

    private Marker removeMarker = MarkerFactory.getMarker("RemoveCartItem");

    @Autowired
    private CartsService cartService;

    @Autowired
    private CartItemsRepository cartItemsRepository;


    @Transactional
    public CartItem addItemToCart(UUID userId, UUID productId, Integer quantity) {
        logger.info(addMarker, "Method addItemToCart entered");
        Cart cart = cartService.getCart(userId);

        if (quantity <= 0) {
            logger.error(addMarker, "Invalid quantity to add cartItem");
            throw new InvalidCartItemException("Invalid cart item quantity");
        }

        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        CartItem itemToAdd;
        if (item.isPresent()) {
            logger.error(addMarker, "Cart item already exists");
            throw new CartItemAlreadyExistsException("Cart Item already exists");
        } else {
            itemToAdd = new CartItem();
            itemToAdd.setProductId(productId);
            itemToAdd.setQuantity(quantity);
        }

        return cartItemsRepository.save(itemToAdd);
    }

    @Transactional
    public CartItem updateCartItem(UUID userId, UUID productId, Integer quantity) {
        logger.info(updateMarker, "Method updateCartItem entered");
        Cart cart = cartService.getCart(userId);

        if (quantity <= 0) {
            logger.error(updateMarker, "Invalid quantity to update cartItem");
            throw new InvalidCartItemException("Invalid cart item quantity");
        }

        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        CartItem i;

        if (item.isPresent()) {
            i = item.get();
            i.setQuantity(quantity);
        } else {
            logger.error(updateMarker, "Cart item not found");
            throw new CartItemNotFoundException("Cart Item not found");
        }

        return cartItemsRepository.save(i);
    }


    @Transactional
    public void removeItemFromCart(UUID userId, UUID productId) {
        logger.info(removeMarker, "Method removeItemFromCart entered");
        Cart cart = cartService.getCart(userId);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Item not found in cart"));


        cartItemsRepository.delete(itemToRemove);
    }


}
