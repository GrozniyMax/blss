package com.blss.blss.controller;

import com.blss.blss.dto.input.OrderCreateRequestDTO;
import com.blss.blss.dto.output.DtoMapper;
import com.blss.blss.dto.output.GetOrderResponse;
import com.blss.blss.dto.output.OrderCreationResponse;
import com.blss.blss.service.camunda.CamundaProcessClient;
import com.blss.blss.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Обработчик связанный с заказами
 */
@RestController
@RequestMapping("/blss/order")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderController {

    OrderService orderService;

    CamundaProcessClient camundaProcessClient;

    ObjectMapper objectMapper;

    DtoMapper dtoMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@orderSecurityService.canCreateOrderFor(#order.owner)")
    public OrderCreationResponse createOrder(
            @RequestBody OrderCreateRequestDTO order
    ) {
        log.info("Creating order: owner={}, location={}, productIds={}", order.owner(), order.location(), order.productIds());
        var variables = camundaProcessClient.startAndAwait("createOrderProcess", java.util.Map.of(
                "owner", order.owner(),
                "location", order.location().toString(),
                "productIds", toJson(order.productIds())
        ), java.util.Set.of("orderId"));
        var orderId = UUID.fromString(variables.get("orderId").toString());
        log.info("Order created successfully via Camunda: orderId={}", orderId);
        return new OrderCreationResponse(orderId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@orderSecurityService.canAccessOrder(#id, authentication.name)")
    public GetOrderResponse getOrderById(@PathVariable UUID id) {
        log.info("Getting order: id={}", id);
        var order = orderService.getOrderContentById(id);
        log.info("Order retrieved successfully: id={}", id);
        return dtoMapper.toDto(order);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/status/next")
    @PreAuthorize("@orderSecurityService.canAccessOrder(#id, authentication.name)")
    public void nextStatus(
            @PathVariable UUID id
    ) {
        log.info("Advancing order status: id={}", id);
        camundaProcessClient.startAndAwait("orderStatusProcess", java.util.Map.of(
                "orderId", id.toString(),
                "action", "NEXT"
        ), java.util.Set.of("processSuccess"));
        log.info("Order status advanced successfully via Camunda: id={}", id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/status/cancel")
    @PreAuthorize("@orderSecurityService.canAccessOrder(#id, authentication.name)")
    public void cancelOrder(
            @PathVariable UUID id
    ) {
        log.info("Cancelling order: id={}", id);
        camundaProcessClient.startAndAwait("orderStatusProcess", java.util.Map.of(
                "orderId", id.toString(),
                "action", "CANCEL"
        ), java.util.Set.of("processSuccess"));
        log.info("Order cancelled successfully via Camunda: id={}", id);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize process variable", e);
        }
    }
}
