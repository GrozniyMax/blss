package com.blss.statusservice.service;

import com.blss.statusservice.client.UserServiceClient;
import com.blss.statusservice.db.OrderStatusHistoryRepo;
import com.blss.statusservice.domain.OrderStatusHistory;
import com.blss.statusservice.dto.output.OrderStatusHistoryResponse;
import com.blss.statusservice.dto.output.StatusHistoryEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for order status history management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepo historyRepo;
    private final UserServiceClient userServiceClient;

    /**
     * Get status history for an order.
     * Validates that the user exists before returning history.
     *
     * @param orderId Order ID
     * @param username Username requesting the history
     * @return Order status history response
     * @throws IllegalArgumentException if user doesn't exist
     */
    public OrderStatusHistoryResponse getStatusHistory(UUID orderId, String username) {
        log.info("Getting status history for order: {}, user: {}", orderId, username);

        // Validate user exists via REST call to user-service
        if (!userServiceClient.existsByUsername(username)) {
            log.warn("User not found: {}", username);
            throw new IllegalArgumentException("User not found: " + username);
        }

        List<OrderStatusHistory> history = historyRepo.findByOrderId(orderId);

        log.info("Found {} status entries for order: {}", history.size(), orderId);

        List<StatusHistoryEntryDto> historyDto = history.stream()
                .map(entry -> new StatusHistoryEntryDto(
                        entry.status(),
                        entry.changedAt(),
                        entry.changedBy()
                ))
                .toList();

        return new OrderStatusHistoryResponse(orderId, historyDto);
    }

    /**
     * Save status change to history.
     * Called when order status changes.
     *
     * @param orderId Order ID
     * @param status New status
     * @param changedBy User who changed the status
     */
    public void saveStatusChange(UUID orderId, String status, String changedBy) {
        log.info("Saving status change: orderId={}, status={}, changedBy={}", orderId, status, changedBy);

        OrderStatusHistory history = new OrderStatusHistory(
                UUID.randomUUID(),
                orderId,
                status,
                Instant.now(),
                changedBy != null ? changedBy : "SYSTEM"
        );

        historyRepo.save(history);
        log.info("Status change saved: orderId={}", orderId);
    }
}
