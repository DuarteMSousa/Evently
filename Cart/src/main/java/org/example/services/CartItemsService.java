package org.example.services;

import feign.FeignException;
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

    /**
     * Adds a new item to the user's cart.
     *
     * @param userId    identifier of the user who owns the cart
     * @param productId identifier of the product/session tier to add
     * @param quantity  number of units to add (must be > 0)
     * @return the persisted CartItem
     * @throws InvalidCartItemException       if quantity is invalid (<= 0)
     * @throws ProductNotFoundException       if the product/session tier does not exist in EventsService
     * @throws ExternalServiceException       if EventsService fails with an unexpected error (FeignException)
     * @throws CartItemAlreadyExistsException if the cart already contains this productId
     */
    @Transactional
    public CartItem addItemToCart(UUID userId, UUID productId, Integer quantity) {
        logger.info(ITEM_ADD, "Method addItemToCart entered");
        Cart cart = cartService.getCart(userId);

        if (quantity <= 0) {
            logger.error(ITEM_ADD, "Invalid quantity to add cartItem");
            throw new InvalidCartItemException("Invalid cart item quantity");
        }

        SessionTierDTO tier;

        try {
            tier = eventsClient.getSessionTier(productId).getBody();
        } catch (FeignException.NotFound e) {
            String errorBody = e.contentUTF8();
            logger.error(ITEM_ADD, "Not found response while getting session tier from EventsService: {}", errorBody);
            throw new ProductNotFoundException("Session tier not found");
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(ITEM_ADD, "FeignException while getting session tier from EventsService: {}", errorBody);
            throw new ExternalServiceException("Error while getting session tier from EventsService");
        }

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
            itemToAdd.setCart(cart);
        }

        return cartItemsRepository.save(itemToAdd);
    }

    /**
     * Updates the quantity of an existing cart item.
     *
     * @param userId    identifier of the user who owns the cart
     * @param productId identifier of the product/session tier to update
     * @param quantity  new quantity (must be > 0)
     * @return the updated persisted CartItem
     * @throws InvalidCartItemException  if quantity is invalid (<= 0)
     * @throws CartItemNotFoundException if the item does not exist in the cart
     */
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

    /**
     * Removes an existing item from the user's cart.
     *
     * @param userId    identifier of the user who owns the cart
     * @param productId identifier of the product/session tier to remove
     * @throws CartItemNotFoundException if the item is not present in the cart
     */
    @Transactional
    public void removeItemFromCart(UUID userId, UUID productId) {
        logger.info(ITEM_REMOVE, "Method removeItemFromCart entered");
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem itemToRemove = cartItemsRepository.findByCartAndProductId(cart,productId).orElse(null);

        if (itemToRemove == null) {
            logger.error(ITEM_REMOVE, "Item not found in cart");
            throw new CartItemNotFoundException("Item not found in cart");
        }

        cartItemsRepository.delete(itemToRemove);
    }
}
