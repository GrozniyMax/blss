package com.blss.blss.controller;

import com.blss.blss.domain.order.Status;
import com.blss.blss.dto.OrderCreateRequestDTO;
import com.blss.blss.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {
    OrderService orderService;

    @PostMapping("/create")
    public OrderService.CreationOrderResponse createOrder(
            @RequestBody OrderCreateRequestDTO order
    ) {
        return orderService.createOrder(order.owner(), order.location(), order.productIds());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestParam Status status
    ) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

}
