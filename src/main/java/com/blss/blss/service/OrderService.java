package com.blss.blss.service;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.db.order.OrderRepo;
import com.blss.blss.db.StoreRepo;
import com.blss.blss.domain.Product;
import com.blss.blss.domain.order.Order;
import com.blss.blss.domain.order.OrderItem;
import com.blss.blss.domain.order.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    ProductRepo productRepo;

    StoreRepo storeRepo;

    OrderRepo orderRepo;

    OrderItemRepo orderItemRepo;


    /**
     * Создание заказа
     */
    public CreationOrderResponse createOrder(UUID owner, UUID location, List<UUID> productIds) {

        var totalPrice = StreamSupport.stream(productRepo.findAllById(productIds).spliterator(), false)
                .map(Product::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var order = Order.builder()
                .owner(owner)
                .localtion(location)
                .creationDate(Instant.now())
                .status(Status.CREATED)
                .totalAmount(totalPrice)
                .build();

        order = orderRepo.create(order);
        var orderId = order.id();

        var positions = productIds.stream()
                .map(id ->
                        new OrderItem(
                                null,
                                orderId,
                                id,
                                null
                        )
                )
                .toList();

        var ids = orderItemRepo.create(positions);

        return new CreationOrderResponse(orderId, ids);
    }

    public void updateStatus(UUID id, Status status) {
        orderRepo.updateStatus(id, status);
    }

    public record CreationOrderResponse(
            UUID orderId,
            List<UUID> positions
    ) { }
}
