package org.example.services;

import org.example.clients.OrdersClient;
import org.example.dtos.externalServices.orders.OrderCreateDTO;
import org.example.exceptions.CartAlreadyExistsException;
import org.example.exceptions.CartNotFoundException;
import org.example.exceptions.EmptyCartException;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartsServiceTest {

    @Mock private CartsRepository cartsRepository;
    @Mock private OrdersClient ordersClient;

    @InjectMocks private CartsService cartsService;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
    }

    @Test
    void getCart_notFound_createsNewCart() {
        when(cartsRepository.findById(userId)).thenReturn(Optional.empty());
        when(cartsRepository.existsById(userId)).thenReturn(false);
        when(cartsRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = cartsService.getCart(userId);

        assertNotNull(cart);
        assertEquals(userId, cart.getUserId());
        verify(cartsRepository).save(any(Cart.class));
    }

    @Test
    void createCart_alreadyExists_throwsCartAlreadyExistsException() {
        when(cartsRepository.existsById(userId)).thenReturn(true);

        CartAlreadyExistsException ex = assertThrows(CartAlreadyExistsException.class,
                () -> cartsService.createCart(userId));

        assertEquals("Cart already exists", ex.getMessage());
        verify(cartsRepository, never()).save(any());
    }

    @Test
    void clearCart_notFound_throwsCartNotFoundException() {
        when(cartsRepository.findById(userId)).thenReturn(Optional.empty());

        CartNotFoundException ex = assertThrows(CartNotFoundException.class,
                () -> cartsService.clearCart(userId));

        assertEquals("Cart not found", ex.getMessage());
    }

    @Test
    void clearCart_success_emptiesItemsAndSaves() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>(Collections.singletonList(new CartItem())));

        when(cartsRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(cartsRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartsService.clearCart(userId);

        assertTrue(result.getItems().isEmpty());
        verify(cartsRepository).save(cart);
    }

    @Test
    void checkoutCart_cartNotFound_throwsCartNotFoundException() {
        when(cartsRepository.findById(userId)).thenReturn(Optional.empty());

        CartNotFoundException ex = assertThrows(CartNotFoundException.class,
                () -> cartsService.checkoutCart(userId));

        assertEquals("Cart not found", ex.getMessage());
    }

    @Test
    void checkoutCart_emptyCart_throwsEmptyCartException() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());

        when(cartsRepository.findById(userId)).thenReturn(Optional.of(cart));

        EmptyCartException ex = assertThrows(EmptyCartException.class,
                () -> cartsService.checkoutCart(userId));

        assertEquals("Empty Cart", ex.getMessage());
        verifyNoInteractions(ordersClient);
    }

    @Test
    void checkoutCart_success_registersOrder_andClearsCart() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());

        CartItem item = new CartItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(2);
        cart.getItems().add(item);

        when(cartsRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(cartsRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartsService.checkoutCart(userId);

        verify(ordersClient).registerOrder(any(OrderCreateDTO.class));
        assertTrue(result.getItems().isEmpty());
        verify(cartsRepository).save(cart);
    }
}
