package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.OrdersClient;
import org.example.dtos.externalServices.orderLines.OrderLineCreateDTO;
import org.example.dtos.externalServices.orders.OrderCreateDTO;
import org.example.exceptions.CartAlreadyExistsException;
import org.example.exceptions.CartNotFoundException;
import org.example.exceptions.EmptyCartException;
import org.example.exceptions.ExternalServiceException;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartItemsRepository;
import org.example.repositories.CartsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class CartsService {

    private Logger logger = LoggerFactory.getLogger(CartsService.class);

    private static final Marker CART_GET = MarkerFactory.getMarker("CART_GET");
    private static final Marker CART_CLEAR = MarkerFactory.getMarker("CART_CLEAR");
    private static final Marker CART_CREATE = MarkerFactory.getMarker("CART_CREATE");
    private static final Marker CART_CHECKOUT = MarkerFactory.getMarker("CART_CHECKOUT");

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private CartItemsRepository cartItemsRepository;

    @Autowired
    private OrdersClient ordersClient;

    /**
     * Retrieves the user's cart.
     *
     *
     * @param userId identifier of the user who owns the cart
     * @return existing cart or a newly created cart
     */
    @Transactional
    public Cart getCart(UUID userId) {
        logger.info(CART_GET, "Method getCart entered");
        Cart cart = cartsRepository.findById(userId).orElse(null);
        if (cart == null) {
            logger.info(CART_GET, "Cart not found, creating a new one");
            cart = createCart(userId);
        }

        cart.setItems(cartItemsRepository.getCartItemsByCart(cart));

        return cart;
    }

    /**
     * Creates a new cart for a user.
     *
     *
     * @param userId identifier of the user that will own the cart
     * @return persisted cart
     *
     * @throws CartAlreadyExistsException if a cart already exists for the given userId
     */
    @Transactional
    public Cart createCart(UUID userId) {
        logger.info(CART_CREATE, "CreateCart Method entered");

        if (cartsRepository.existsById(userId)) {
            logger.error(CART_CREATE, "Cart for user {} already exists", userId);
            throw new CartAlreadyExistsException("Cart already exists");
        }

        Cart newCart = new Cart();
        newCart.setUserId(userId);

        newCart = cartsRepository.save(newCart);

        return newCart;
    }

    /**
     * Clears all items from the user's cart.
     *
     *
     * @param userId identifier of the user who owns the cart
     * @return updated cart with an empty items list
     *
     * @throws CartNotFoundException if the cart does not exist
     */
    @Transactional
    public Cart clearCart(UUID userId) {
        logger.info(CART_CLEAR, "clearCart method entered");

        Cart cartToClear = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        cartToClear.setItems(new ArrayList<CartItem>());

        return cartsRepository.save(cartToClear);
    }

    /**
     * Performs checkout of the user's cart by creating an order in OrdersService.
     *
     *
     * @param userId identifier of the user who owns the cart
     * @return updated cart with items cleared
     *
     * @throws CartNotFoundException     if the cart does not exist
     * @throws EmptyCartException        if the cart exists but has no items
     * @throws ExternalServiceException  if OrdersService returns an error (FeignException)
     */
    @Transactional
    public Cart checkoutCart(UUID userId) {
        logger.info(CART_CHECKOUT, "checkoutCart method entered");

        Cart cartToCheckout = cartsRepository
                .findById(userId)
                .orElse(null);

        if (cartToCheckout == null) {
            logger.error(CART_CHECKOUT, "Cart not found");
            throw new CartNotFoundException("Cart not found");
        }

        if (cartToCheckout.getItems().isEmpty()) {
            logger.error(CART_CHECKOUT, "Empty Cart");
            throw new EmptyCartException("Empty Cart");
        }

        OrderCreateDTO newOrder = new OrderCreateDTO();
        newOrder.setUserId(userId);

        for (CartItem item : cartToCheckout.getItems()) {
            OrderLineCreateDTO line = new OrderLineCreateDTO();

            line.setQuantity(item.getQuantity());
            line.setProductId(item.getProductId());
            newOrder.getLines().add(line);
        }

        try {
            ordersClient.registerOrder(newOrder);
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(CART_CHECKOUT, "FeignException while registering order in OrdersService: {}", errorBody);
            throw new ExternalServiceException("Error while registering order in OrdersService");
        }

        cartToCheckout.setItems(new ArrayList<CartItem>());

        return cartsRepository.save(cartToCheckout);
    }
}
