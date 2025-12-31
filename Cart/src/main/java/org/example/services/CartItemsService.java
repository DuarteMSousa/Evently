package org.example.services;

import jakarta.transaction.Transactional;
import org.example.clients.EventsClient;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
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

    private static final Marker ITEM_ADD = MarkerFactory.getMarker("ITEM_ADD");
    private static final Marker ITEM_UPDATE = MarkerFactory.getMarker("ITEM_UPDATE");
    private static final Marker ITEM_REMOVE = MarkerFactory.getMarker("ITEM_REMOVE");

    @Autowired
    private CartsService cartService;

    @Autowired
    private CartItemsRepository cartItemsRepository;

    @Autowired
    private EventsClient eventsClient;

    @Transactional
    public CartItem addItemToCart(UUID userId, UUID productId, Integer quantity) {
        logger.info(ITEM_ADD, "Method addItemToCart entered");
        Cart cart = cartService.getCart(userId);

        if (quantity <= 0) {
            logger.error(ITEM_ADD, "Invalid quantity to add cartItem");
            throw new InvalidCartItemException("Invalid cart item quantity");
        }

        SessionTierDTO tier = eventsClient.getSessionTier(productId).getBody();

        if (tier == null) {
            logger.error(ITEM_ADD, "Product not found");
            throw new ProductNotFoundException("Product not found");
        }

        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        CartItem itemToAdd;
        if (item.isPresent()) {
            logger.error(ITEM_ADD, "Cart item already exists");
            throw new CartItemAlreadyExistsException("Cart Item already exists");
        } else {
            itemToAdd = new CartItem();
            itemToAdd.setProductId(productId);
            itemToAdd.setQuantity(quantity);
            itemToAdd.setUnitPrice(tier.getPrice());
        }

        return cartItemsRepository.save(itemToAdd);
    }

    @Transactional
    public CartItem updateCartItem(UUID userId, UUID productId, Integer quantity) {
        logger.info(ITEM_UPDATE, "Method updateCartItem entered");
        Cart cart = cartService.getCart(userId);

        if (quantity <= 0) {
            logger.error(ITEM_UPDATE, "Invalid quantity to update cartItem");
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
            logger.error(ITEM_UPDATE, "Cart item not found");
            throw new CartItemNotFoundException("Cart Item not found");
        }

        return cartItemsRepository.save(i);
    }


    @Transactional
    public void removeItemFromCart(UUID userId, UUID productId) {
        logger.info(ITEM_REMOVE, "Method removeItemFromCart entered");
        Cart cart = cartService.getCart(userId);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);


        if (itemToRemove == null) {
            logger.error(ITEM_REMOVE, "Item not found in cart");
            throw new CartItemNotFoundException("Item not found in cart");
        }

        cartItemsRepository.delete(itemToRemove);
    }


}
