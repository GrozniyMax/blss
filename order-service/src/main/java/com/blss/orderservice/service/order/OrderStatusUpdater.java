package com.blss.orderservice.service.order;

import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.exception.InvalidActionException;
import com.blss.orderservice.jms.OrderStatusProducer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderStatusUpdater {

    OrderItemRepo orderItemRepo;

    OrderService orderService;

    OrderStatusProducer statusProducer;

    OrderDocumentSyncService orderDocumentSyncService;

    @Async
    public void updateStatusIfReady(UUID orderId) {
        if (orderService.getStatus(orderId) == Status.IN_DELIVERY
                && orderItemRepo.countItemsWithoutYacheyka(orderId) == 0) {
            orderService.updateStatus(orderId, Status.READY_FOR_PICKUP);
            statusProducer.sendStatusChange(orderId, Status.READY_FOR_PICKUP);
            orderDocumentSyncService.sendOrderDocument(orderId);
        }
    }

    public void next(UUID orderId) {
        var next = next(orderService.getStatus(orderId));

        if (next != null) {
            orderService.updateStatus(orderId, next);
            statusProducer.sendStatusChange(orderId, next);
            orderDocumentSyncService.sendOrderDocument(orderId);
        } else {
            throw new InvalidActionException("Заказ уже в конечном статусе");
        }

    }


    public void revertStatus(UUID orderId, Status revertFrom) {
        var previous = revert(revertFrom);

        var current = orderService.getStatus(orderId);

        if (previous != null && current == revertFrom) {
            orderService.updateStatus(orderId, previous);
            statusProducer.sendStatusChange(orderId, previous);
            orderDocumentSyncService.sendOrderDocument(orderId);
            return;
        }

        log.error("Failed to revert status: orderId={}, currentStatus={}, revertFrom={}",
                orderId, current, revertFrom);
    }

    public void cancel(UUID orderId) {
        var current = orderService.getStatus(orderId);
        if (current != Status.DONE && current != Status.CANCELED) {
            orderService.updateStatus(orderId, Status.CANCELED);
            statusProducer.sendStatusChange(orderId, Status.CANCELED);
            orderDocumentSyncService.sendOrderDocument(orderId);
        } else {
            throw new InvalidActionException("Заказ уже доставлен, его нельзя отменить");
        }
    }

    private Status next(Status status) {
        return switch (status) {
            case CREATED -> Status.PROCESSING;
            case PROCESSING -> Status.IN_DELIVERY;
            case IN_DELIVERY -> Status.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> Status.DONE;
            case CANCELED, DONE -> null;
        };
    }

    private Status revert(Status status) {
        return switch (status) {
            case CREATED -> null;
            case PROCESSING -> Status.CREATED;
            case IN_DELIVERY -> Status.PROCESSING;
            case READY_FOR_PICKUP -> Status.IN_DELIVERY;
            case CANCELED, DONE -> Status.READY_FOR_PICKUP;
        };
    }
}
