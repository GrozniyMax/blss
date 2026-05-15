package com.blss.orderservice.dto.output;

import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.service.order.OrderService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetOrderResponse (
    UUID id,
    String owner,
    Instant creationDate,
    Status status,
    BigDecimal totalAmount,
    List<OrderService.FullOrderItem> positions

) { }
