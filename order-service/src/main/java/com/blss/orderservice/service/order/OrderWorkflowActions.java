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
import com.blss.orderservice.exception.InvalidActionException;
import com.blss.orderservice.exception.InvalidOrderException;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.jms.OrderStatusProducer;
import com.blss.orderservice.service.tx.TransactionExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderWorkflowActions {

    ProductRepo productRepo;
    StoreRepo storeRepo;
    OrderRepo orderRepo;
    OrderItemRepo orderItemRepo;
    DeliveryPointRepo deliveryPointRepo;
    UserServiceClient userServiceClient;
    TransactionExecutor transactionExecutor;
    OrderStatusProducer orderStatusProducer;
    OrderDocumentSyncService orderDocumentSyncService;

    public void createOrder(DelegateExecution execution) {
        var owner = (String) execution.getVariable("owner");
        var location = UUID.fromString((String) execution.getVariable("location"));
        var productIds = productIds(execution);

        var response = transactionExecutor.inTransaction(() -> {
            var foundProduct = StreamSupport.stream(productRepo.findAllById(productIds).spliterator(), false).toList();

            if (foundProduct.size() != productIds.size()) {
                throw new InvalidOrderException("РќРµ РІСЃРµ РїСЂРѕРґСѓРєС‚С‹ Р±С‹Р»Рё РЅР°Р№РґРµРЅС‹");
            }

            if (deliveryPointRepo.findById(location).isEmpty()) {
                throw new InvalidOrderException("РџР’Р— РЅРµ СЃСѓС‰РµСЃС‚РІСѓРµС‚");
            }

            if (!userServiceClient.existsByUsername(owner)) {
                throw new InvalidOrderException("РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ РЅРµ СЃСѓС‰РµСЃС‚РІСѓРµС‚");
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
                    .map(id -> new OrderItem(null, orderId, id, null))
                    .toList();

            var ids = positions.stream()
                    .map(orderItemRepo::create)
                    .map(OrderItem::id)
                    .toList();

            orderStatusProducer.sendStatusChange(orderId, Status.CREATED);
            return new OrderService.CreationOrderResponse(orderId, ids);
        });

        execution.setVariable("orderId", response.orderId().toString());
        execution.setVariable("positionIds", response.positions().stream().map(UUID::toString).toList());
    }

    public void loadStatus(DelegateExecution execution) {
        var orderId = orderId(execution);
        execution.setVariable("currentStatus", orderRepo.findById(orderId)
                .map(Order::status)
                .map(Status::name)
                .orElseThrow(() -> new NotFoundException(Order.class, orderId)));
    }

    public void changeStatus(DelegateExecution execution, String targetStatus) {
        var orderId = orderId(execution);
        var status = Status.valueOf(targetStatus);

        transactionExecutor.inTransaction(() ->
                orderRepo.updateStatus(orderId, status)
                        .orElseThrow(() -> new NotFoundException(Order.class, orderId))
        );

        orderStatusProducer.sendStatusChange(orderId, status);
        orderDocumentSyncService.sendOrderDocument(orderId);
    }

    public void rejectTerminalCancel(DelegateExecution execution) {
        throw new InvalidActionException("Р—Р°РєР°Р· СѓР¶Рµ РґРѕСЃС‚Р°РІР»РµРЅ, РµРіРѕ РЅРµР»СЊР·СЏ РѕС‚РјРµРЅРёС‚СЊ");
    }

    public void rejectTerminalNext(DelegateExecution execution) {
        throw new InvalidActionException("Р—Р°РєР°Р· СѓР¶Рµ РІ РєРѕРЅРµС‡РЅРѕРј СЃС‚Р°С‚СѓСЃРµ");
    }

    public void markDelivered(DelegateExecution execution) {
        var itemId = UUID.fromString((String) execution.getVariable("itemId"));
        var yacheyka = (String) execution.getVariable("yacheyka");
        var item = transactionExecutor.inTransaction(() ->
                orderItemRepo.updateYacheyka(itemId, yacheyka)
                        .orElseThrow(() -> new NotFoundException(OrderItem.class, itemId))
        );
        execution.setVariable("orderId", item.orderId().toString());
    }

    public void loadDeliveryReadiness(DelegateExecution execution) {
        var orderId = orderId(execution);
        var status = orderRepo.findById(orderId)
                .map(Order::status)
                .orElseThrow(() -> new NotFoundException(Order.class, orderId));
        execution.setVariable("currentStatus", status.name());
        execution.setVariable("itemsWithoutCell", orderItemRepo.countItemsWithoutYacheyka(orderId));
    }

    public void sendOrderDocument(DelegateExecution execution) {
        orderDocumentSyncService.sendOrderDocument(orderId(execution));
    }

    private UUID orderId(DelegateExecution execution) {
        return UUID.fromString((String) execution.getVariable("orderId"));
    }

    private List<UUID> productIds(DelegateExecution execution) {
        @SuppressWarnings("unchecked")
        var productIds = (List<String>) execution.getVariable("productIds");
        return productIds.stream().map(UUID::fromString).toList();
    }
}
