package org.example.services;

import org.example.clients.EventsClient;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.example.exceptions.*;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartItemsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemsServiceTest {

    @Mock private CartsService cartsService;
    @Mock private CartItemsRepository cartItemsRepository;
    @Mock private EventsClient eventsClient;

    @InjectMocks private CartItemsService cartItemsService;

    private UUID userId;
    private UUID productId;
    private Cart cart;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
    }

    // -------- addItemToCart --------

    @Test
    void addItemToCart_quantityInvalid_throwsInvalidCartItemException() {
        when(cartsService.getCart(userId)).thenReturn(cart);

        InvalidCartItemException ex = assertThrows(InvalidCartItemException.class,
                () -> cartItemsService.addItemToCart(userId, productId, 0));

        assertEquals("Invalid cart item quantity", ex.getMessage());
        verify(cartItemsRepository, never()).save(any());
    }

    @Test
    void addItemToCart_productNotFound_throwsProductNotFoundException() {
        when(cartsService.getCart(userId)).thenReturn(cart);
        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(null));

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> cartItemsService.addItemToCart(userId, productId, 1));

        assertEquals("Product not found", ex.getMessage());
        verify(cartItemsRepository, never()).save(any());
    }

    @Test
    void addItemToCart_itemAlreadyExists_throwsCartItemAlreadyExistsException() {
        if (cart.getItems() == null) {
            cart.setItems(new java.util.ArrayList<>());
        }

        CartItem existing = new CartItem();
        existing.setProductId(productId);
        existing.setQuantity(1);
        cart.getItems().add(existing);

        when(cartsService.getCart(userId)).thenReturn(cart);

        // Só precisamos que getBody() não seja null
        SessionTierDTO tier = mock(SessionTierDTO.class);
        when(eventsClient.getSessionTier(productId))
                .thenReturn(org.springframework.http.ResponseEntity.ok(tier));

        CartItemAlreadyExistsException ex = assertThrows(CartItemAlreadyExistsException.class,
                () -> cartItemsService.addItemToCart(userId, productId, 1));

        assertEquals("Cart Item already exists", ex.getMessage());
        verify(cartItemsRepository, never()).save(any());
    }


    @Test
    void addItemToCart_success_savesAndReturns() {
        when(cartsService.getCart(userId)).thenReturn(cart);

        SessionTierDTO tier = mock(SessionTierDTO.class);
        when(tier.getPrice()).thenReturn(12.5f);
        when(eventsClient.getSessionTier(productId)).thenReturn(ResponseEntity.ok(tier));

        when(cartItemsRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        CartItem result = cartItemsService.addItemToCart(userId, productId, 2);

        assertNotNull(result.getId());
        assertEquals(productId, result.getProductId());
        assertEquals(2, result.getQuantity());
        assertEquals(12.5f, result.getUnitPrice());
    }

    // -------- updateCartItem --------

    @Test
    void updateCartItem_quantityInvalid_throwsInvalidCartItemException() {
        when(cartsService.getCart(userId)).thenReturn(cart);

        InvalidCartItemException ex = assertThrows(InvalidCartItemException.class,
                () -> cartItemsService.updateCartItem(userId, productId, 0));

        assertEquals("Invalid cart item quantity", ex.getMessage());
        verify(cartItemsRepository, never()).save(any());
    }

    @Test
    void updateCartItem_itemNotFound_throwsCartItemNotFoundException() {
        when(cartsService.getCart(userId)).thenReturn(cart);

        CartItemNotFoundException ex = assertThrows(CartItemNotFoundException.class,
                () -> cartItemsService.updateCartItem(userId, productId, 1));

        assertEquals("Cart Item not found", ex.getMessage());
        verify(cartItemsRepository, never()).save(any());
    }

    @Test
    void updateCartItem_success_updatesAndSaves() {
        CartItem existing = new CartItem();
        existing.setProductId(productId);
        existing.setQuantity(1);
        cart.getItems().add(existing);

        when(cartsService.getCart(userId)).thenReturn(cart);
        when(cartItemsRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartItemsService.updateCartItem(userId, productId, 5);

        assertEquals(5, result.getQuantity());
        verify(cartItemsRepository).save(existing);
    }

    // -------- removeItemFromCart --------

    @Test
    void removeItemFromCart_itemNotFound_throwsCartItemNotFoundException() {
        when(cartsService.getCart(userId)).thenReturn(cart);

        CartItemNotFoundException ex = assertThrows(CartItemNotFoundException.class,
                () -> cartItemsService.removeItemFromCart(userId, productId));

        assertEquals("Item not found in cart", ex.getMessage());
        verify(cartItemsRepository, never()).delete(any());
    }

    @Test
    void removeItemFromCart_success_deletes() {
        CartItem existing = new CartItem();
        existing.setProductId(productId);
        existing.setQuantity(1);
        cart.getItems().add(existing);

        when(cartsService.getCart(userId)).thenReturn(cart);

        cartItemsService.removeItemFromCart(userId, productId);

        verify(cartItemsRepository).delete(existing);
    }
}
