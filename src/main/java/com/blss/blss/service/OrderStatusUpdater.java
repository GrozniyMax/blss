package com.blss.blss.service;

import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.domain.order.Status;
import com.blss.blss.exception.InvalidActionException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderStatusUpdater {

    OrderItemRepo orderItemRepo;

    OrderService orderService;

    @Async
    public void updateStatusIfReady(UUID orderId) {
        if (orderService.getStatus(orderId) == Status.IN_DELIVERY
                && orderItemRepo.countItemsWithoutYacheyka(orderId) == 0) {
            orderService.updateStatus(orderId, Status.READY_FOR_PICKUP);
        }
    }

    public void next(UUID orderId) {
        var next = next(orderService.getStatus(orderId));

        if (next != null) {
            orderService.updateStatus(orderId, next);
        } else {
            throw new InvalidActionException("Заказ уже в конечном статусе");
        }

    }

    public void cancel(UUID orderId) {
        var current = orderService.getStatus(orderId);
        if (current != Status.DONE && current != Status.CANCELED) {
            orderService.updateStatus(orderId, Status.CANCELED);
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
}
