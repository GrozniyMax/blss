package com.blss.statusservice.controller;

import com.blss.statusservice.dto.output.OrderStatusHistoryResponse;
import com.blss.statusservice.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for order status history.
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderStatusHistoryController {

    private final OrderStatusHistoryService historyService;

    /**
     * Get status history for an order.
     * Requires authentication.
     *
     * @param id Order ID
     * @param username Authenticated username
     * @return Status history response
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<OrderStatusHistoryResponse> getOrderHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username
    ) {
        log.info("Getting order history: id={}, user={}", id, username);
        OrderStatusHistoryResponse response = historyService.getStatusHistory(id, username);
        return ResponseEntity.ok(response);
    }
}
