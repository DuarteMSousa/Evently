package org.evently.orders.services;

import jakarta.transaction.Transactional;
import org.evently.orders.exceptions.InvalidOrderUpdateException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.repositories.OrdersRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrdersService {

    @Autowired
    private OrdersRepository ordersRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public Order getOrder(UUID id) {
        return ordersRepository
                .findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Transactional
    public Order createOrder(Order order) {
        return ordersRepository.save(order);
    }

    @Transactional
    public Order updateOrder(UUID id, Order order) {
        if (!id.equals(order.getId())) {
            throw new InvalidOrderUpdateException("Parameter id and body id do not correspond");
        }

        Order existingOrder = ordersRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(order, existingOrder);

        return ordersRepository.save(existingOrder);
    }

    public Page<Order> getOrdersByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        if(pageSize > 50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return ordersRepository.findAllByUserId(userId, pageable);
    }
}
