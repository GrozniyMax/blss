package com.blss.blss.dto.output;

import com.blss.blss.domain.order.Status;
import com.blss.blss.service.OrderService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GetOrderResponse (
    UUID id,
    UUID owner,
    Instant creationDate,
    Status status,
    BigDecimal totalAmount,
    List<OrderService.FullOrderItem> positions

) { }
