package com.blss.orderservice.service.order;

import com.blss.orderservice.client.UserServiceClient;
import com.blss.orderservice.db.DeliveryPointRepo;
import com.blss.orderservice.db.ProductRepo;
import com.blss.orderservice.db.StoreRepo;
import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.Product;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderItem;
import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.exception.InvalidOrderException;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.jms.OrderStatusProducer;
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

    StoreRepo storeRepo;

    OrderRepo orderRepo;

    OrderItemRepo orderItemRepo;

    DeliveryPointRepo deliveryPointRepo;

    UserServiceClient userServiceClient;

    TransactionExecutor transactionExecutor;

    OrderDocumentSyncService orderDocumentSyncService;

    OrderStatusProducer orderStatusProducer;

    /**
     * Creates a new order.
     */
    public CreationOrderResponse createOrder(String owner, UUID location, List<UUID> productIds) {
        var response = transactionExecutor.inTransaction(() -> {
            var foundProduct = StreamSupport.stream(productRepo.findAllById(productIds).spliterator(), false).toList();

            if (foundProduct.size() != productIds.size()) {
                throw new InvalidOrderException("Не все продукты были найдены");
            }

            if (deliveryPointRepo.findById(location).isEmpty()) {
                throw new InvalidOrderException("ПВЗ не существует");
            }

            if (!userServiceClient.existsByUsername(owner)) {
                throw new InvalidOrderException("Пользователь не существует");
            }

                    var totalPrice = foundProduct.stream()
                    .map(Product::price)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var order = Order.builder()
                    .owner(owner)
                    .location(location)
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

            var ids = positions.stream()
                    .map(orderItemRepo::create)
                    .map(OrderItem::id)
                    .toList();

            orderStatusProducer.sendStatusChange(orderId, Status.CREATED);

            return new CreationOrderResponse(orderId, ids);
        });
        orderDocumentSyncService.sendOrderDocument(response.orderId());
        return response;
    }

    public Status getStatus(UUID orderId) {
        return orderRepo.findById(orderId).map(Order::status).orElseThrow(() -> new NotFoundException(Order.class, orderId));
    }

    public void updateStatus(UUID id, Status status) {
        transactionExecutor.inTransaction(() ->
                orderRepo.updateStatus(id, status).orElseThrow(() -> new NotFoundException(Order.class, id))
        );
    }

    /**
     * Reverts order status when status-service fails to process a status change.
     * Moves the order back to CREATED status to allow reprocessing.
     *
     * @param orderId Order ID to revert
     * @param reason  Reason for revert (error message from status-service)
     */
    public void revertStatus(UUID orderId, String reason) {
        log.info("Reverting order status: orderId={}, reason={}", orderId, reason);
        
        transactionExecutor.inTransaction(() -> {
            var order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(Order.class, orderId));
            
            // Revert to CREATED status to allow reprocessing
            // In production, you might want more sophisticated logic based on current status
            orderRepo.updateStatus(orderId, Status.CREATED)
                    .orElseThrow(() -> new NotFoundException(Order.class, orderId));
            
            log.info("Order status reverted to CREATED: orderId={}", orderId);
        });
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
