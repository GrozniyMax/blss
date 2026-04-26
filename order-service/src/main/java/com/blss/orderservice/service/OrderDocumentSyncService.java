package com.blss.orderservice.service;

import com.blss.orderservice.bitrix.BitrixOrderDocument;
import com.blss.orderservice.bitrix.BitrixOrderDocumentClient;
import com.blss.orderservice.db.DeliveryPointRepo;
import com.blss.orderservice.db.ProductRepo;
import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.DeliveryPoint;
import com.blss.orderservice.domain.Product;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderItem;
import com.blss.orderservice.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
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

    BitrixOrderDocumentClient bitrixClient;

    public void sendOrderDocument(UUID orderId) {
        if (!bitrixClient.isEnabled()) {
            log.info("Skipping Bitrix24 sync for order {} because integration is disabled", orderId);
            return;
        }

        try {
            var order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new NotFoundException(Order.class, orderId));
            var deliveryPoint = deliveryPointRepo.findById(order.location())
                    .orElseThrow(() -> new NotFoundException(DeliveryPoint.class, order.location()));
            var items = orderItemRepo.findAllByOrderId(orderId);
            var products = loadProducts(items);

            bitrixClient.createOrderDocument(new BitrixOrderDocument(
                    orderId,
                    "Заказ " + orderId,
                    formatDocument(order, deliveryPoint, items, products),
                    order.totalAmount()
            ));
        } catch (RuntimeException ex) {
            log.error("Bitrix24 sync failed for order {}: {}", orderId, ex.getMessage(), ex);
        }
    }

    private Map<UUID, Product> loadProducts(java.util.List<OrderItem> items) {
        return StreamSupport.stream(
                        productRepo.findAllById(items.stream().map(OrderItem::productId).toList()).spliterator(),
                        false
                )
                .collect(Collectors.toMap(Product::id, Function.identity()));
    }

    private String formatDocument(
            Order order,
            DeliveryPoint deliveryPoint,
            java.util.List<OrderItem> items,
            Map<UUID, Product> products
    ) {
        var lines = items.stream()
                .sorted(Comparator.comparing(OrderItem::id))
                .map(item -> {
                    var product = products.get(item.productId());
                    var productName = product != null ? product.name() : "Unknown product";
                    var price = product != null ? product.price().toPlainString() : "0";
                    var cell = item.yacheyka() != null ? item.yacheyka() : "не назначена";
                    return "- " + productName + " | цена: " + price + " | ячейка: " + cell;
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
