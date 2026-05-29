package com.blss.orderservice.service.order;

import com.blss.orderservice.db.ProductRepo;
import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.Product;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderItem;
import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.service.tx.TransactionExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
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

    OrderRepo orderRepo;

    OrderItemRepo orderItemRepo;

    TransactionExecutor transactionExecutor;

    OrderProcessService orderProcessService;

    public CreationOrderResponse createOrder(String owner, UUID location, List<UUID> productIds) {
        return orderProcessService.createOrder(owner, location, productIds);
    }

    public Status getStatus(UUID orderId) {
        return orderRepo.findById(orderId).map(Order::status).orElseThrow(() -> new NotFoundException(Order.class, orderId));
    }

    public void updateStatus(UUID id, Status status) {
        transactionExecutor.inTransaction(() ->
                orderRepo.updateStatus(id, status).orElseThrow(() -> new NotFoundException(Order.class, id))
        );
    }

    public FullOrder getOrderContentById(UUID id) {
        var order = orderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(Order.class, id));

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
    ) {
    }

    public record FullOrder(
            UUID id,
            String owner,
            Instant creationDate,
            Status status,
            BigDecimal totalAmount,
            List<FullOrderItem> positions
    ) {
    }

    public record FullOrderItem(
            UUID id,
            String yacheyka,
            Product product
    ) {
    }
}
