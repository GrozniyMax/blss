package com.blss.orderservice.service.order;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixConnectionFactory;
import com.blss.bitrixjca.api.BitrixOrderItem;
import com.blss.orderservice.db.DeliveryPointRepo;
import com.blss.orderservice.db.ProductRepo;
import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.DeliveryPoint;
import com.blss.orderservice.domain.Product;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderItem;
import com.blss.orderservice.exception.NotFoundException;
import jakarta.resource.ResourceException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderDocumentSyncService {

    OrderRepo orderRepo;

    OrderItemRepo orderItemRepo;

    ProductRepo productRepo;

    DeliveryPointRepo deliveryPointRepo;

    BitrixConnectionFactory connectionFactory;

    public void sendOrderDocument(UUID orderId) {
        log.debug("Starting Bitrix24 sync for order {}", orderId);

        try (BitrixConnection connection = connectionFactory.getConnection()) {
            if (!connection.isValid()) {
                log.warn("Bitrix24 connection is not valid, skipping sync for order {}", orderId);
                return;
            }

            var order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(Order.class, orderId));
            var deliveryPoint = deliveryPointRepo.findById(order.location())
                    .orElseThrow(() -> new NotFoundException(DeliveryPoint.class, order.location()));
            var items = orderItemRepo.findAllByOrderId(orderId);
            var products = loadProducts(items);
            var documentItems = buildDocumentItems(items, products);

            String result = connection.createDocument(
                    orderId,
                    "Заказ " + orderId,
                    formatDocument(order, deliveryPoint, documentItems),
                    order.totalAmount(),
                    order.owner(),
                    order.status().name(),
                    order.creationDate(),
                    deliveryPoint.name(),
                    deliveryPoint.address(),
                    documentItems
            );

            log.info("Bitrix24 sync completed for order {}: {}", orderId, result);
        } catch (ResourceException e) {
            log.error("Bitrix24 sync failed for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    private Map<UUID, Product> loadProducts(List<OrderItem> items) {
        return StreamSupport.stream(
                        productRepo.findAllById(items.stream().map(OrderItem::productId).toList()).spliterator(),
                        false
                )
                .collect(Collectors.toMap(Product::id, Function.identity()));
    }

    private List<BitrixOrderItem> buildDocumentItems(
            List<OrderItem> items,
            Map<UUID, Product> products
    ) {
        return items.stream()
                .sorted(Comparator.comparing(OrderItem::id))
                .map(item -> {
                    Product product = products.get(item.productId());
                    return new BitrixOrderItem(
                            item.id(),
                            item.productId(),
                            product != null ? product.name() : "Unknown product",
                            product != null ? product.price() : BigDecimal.ZERO,
                            item.yacheyka()
                    );
                })
                .toList();
    }

    private String formatDocument(
            Order order,
            DeliveryPoint deliveryPoint,
            List<BitrixOrderItem> items
    ) {
        var lines = items.stream()
                .map(item -> {
                    var cell = Objects.toString(item.yacheyka(), "не назначена");
                    return "- " + item.productName()
                            + " | цена: " + item.price().toPlainString()
                            + " | ячейка: " + cell;
                })
                .collect(Collectors.joining(System.lineSeparator()));

        return String.join(System.lineSeparator(),
                "Документ по заказу",
                "ID заказа: " + order.id(),
                "Статус: " + order.status(),
                "Клиент: " + order.owner(),
                "Дата создания: " + order.creationDate(),
                "ПВЗ: " + deliveryPoint.name(),
                "Адрес ПВЗ: " + deliveryPoint.address(),
                "Сумма: " + order.totalAmount().toPlainString(),
                "Состав заказа:",
                lines
        );
    }
}
