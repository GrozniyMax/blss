package com.blss.blss.service;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.db.order.OrderRepo;
import com.blss.blss.db.StoreRepo;
import com.blss.blss.domain.Product;
import com.blss.blss.domain.order.Order;
import com.blss.blss.domain.order.OrderItem;
import com.blss.blss.domain.order.Status;
import com.blss.blss.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    private static final Product NOT_FOUND_PRODUCT = new Product(
            null,
            "Not found",
            BigDecimal.ZERO
    );

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

        storeRepo.decrementCount(productIds);

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
        var order = orderRepo.updateStatus(id, status);

        if (order == null) {
            throw new NotFoundException(Order.class, "Order not found");
        }
    }

    public FullOrder getOrderContentById(UUID id) {
        var order = orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(Order.class, "Order not found"));

        var items = orderItemRepo.findAllByOrderId(id);

        var productMap = StreamSupport.stream(
                productRepo.findAllById(items.stream().map(OrderItem::productId).toList()).spliterator(), false)
                .collect(Collectors.toMap(Product::id, Function.identity()));

        var positions = items.stream()
                .map(item -> {
                    var product = productMap.getOrDefault(item.productId(), NOT_FOUND_PRODUCT);
                    return new FullOrderItem(item.id(), item.yacheyka(), product);
                }).toList();

        return new FullOrder(
                order.id(),
                order.owner(),
                order.creationDate(),
                order.status(),
                order.totalAmount(),
                positions
        );
    }

    public record CreationOrderResponse(
            UUID orderId,
            List<UUID> positions
    ) { }

    public record FullOrder (
            UUID id,
            UUID owner,
            Instant creationDate,
            Status status,
            BigDecimal totalAmount,
            List<FullOrderItem> positions
    ) { }

    public record FullOrderItem (
            UUID id,
            String yacheyka,
            Product product
    ) { }



}
