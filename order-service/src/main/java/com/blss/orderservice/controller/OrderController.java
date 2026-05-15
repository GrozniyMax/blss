package com.blss.orderservice.controller;

import com.blss.orderservice.dto.input.OrderCreateRequestDTO;
import com.blss.orderservice.dto.output.DtoMapper;
import com.blss.orderservice.dto.output.GetOrderResponse;
import com.blss.orderservice.dto.output.OrderCreationResponse;
import com.blss.orderservice.service.order.OrderDocumentSyncService;
import com.blss.orderservice.service.order.OrderService;
import com.blss.orderservice.service.order.OrderStatusUpdater;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Управление заказами.
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {

    OrderService orderService;

    OrderStatusUpdater orderStatusUpdater;

    OrderDocumentSyncService orderDocumentSyncService;

    DtoMapper dtoMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreationResponse createOrder(
            @RequestBody OrderCreateRequestDTO order
    ) {
        log.info("Creating order: owner={}, location={}, productIds={}", order.owner(), order.location(), order.productIds());
        var creationResponse = orderService.createOrder(order.owner(), order.location(), order.productIds());
        log.info("Order created successfully: orderId={}", creationResponse.orderId());
        return new OrderCreationResponse(creationResponse.orderId());
    }

    @GetMapping("/{id}")
    public GetOrderResponse getOrderById(@PathVariable UUID id) {
        log.info("Getting order: id={}", id);
        var order = orderService.getOrderContentById(id);
        log.info("Order retrieved successfully: id={}", id);
        return dtoMapper.toDto(order);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/status/next")
    public void nextStatus(
            @PathVariable UUID id
    ) {
        log.info("Advancing order status: id={}", id);
        orderStatusUpdater.next(id);
        log.info("Order status advanced successfully: id={}", id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/status/cancel")
    public void cancelOrder(
            @PathVariable UUID id
    ) {
        log.info("Cancelling order: id={}", id);
        orderStatusUpdater.cancel(id);
        log.info("Order cancelled successfully: id={}", id);
    }

    @PostMapping("/{id}/bitrix-document")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('CONSULTANT', 'MANAGER', 'ADMIN')")
    public void syncOrderDocumentToBitrix(@PathVariable UUID id) {
        log.info("Syncing order document to Bitrix24: id={}", id);
        orderDocumentSyncService.sendOrderDocument(id);
        log.info("Order document sync finished: id={}", id);
    }
}
