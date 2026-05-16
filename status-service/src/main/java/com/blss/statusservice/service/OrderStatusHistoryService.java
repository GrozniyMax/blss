package com.blss.statusservice.service;

import com.blss.statusservice.client.UserServiceClient;
import com.blss.statusservice.db.OrderStatusHistoryRepo;
import com.blss.statusservice.domain.OrderStatusHistory;
import com.blss.statusservice.dto.OrderStatusHistoryResponse;
import com.blss.statusservice.dto.StatusHistoryEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * История изменений статусов заказов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepo historyRepo;
    private final UserServiceClient userServiceClient;

    public OrderStatusHistoryResponse getStatusHistory(UUID orderId, String username) {
        log.info("Getting status history for order: {}, user: {}", orderId, username);

        if (!userServiceClient.existsByUsername(username)) {
            log.warn("User not found: {}", username);
            throw new IllegalArgumentException("User not found: " + username);
        }

        List<OrderStatusHistory> history = historyRepo.findByOrderId(orderId);

        log.info("Found {} status entries for order: {}", history.size(), orderId);

        List<StatusHistoryEntryDto> historyDto = history.stream()
                .map(entry -> new StatusHistoryEntryDto(
                        entry.status(),
                        entry.changedAt()
                ))
                .toList();

        return new OrderStatusHistoryResponse(orderId, historyDto);
    }

    public void saveStatusChange(UUID orderId, String status, Instant timestamp) {
        log.info("Saving status change: orderId={}, status={}", orderId, status);

        OrderStatusHistory history = new OrderStatusHistory(
                null,
                orderId,
                status,
                timestamp
        );

        historyRepo.save(history);
        log.info("Status change saved: orderId={}", orderId);
    }

}
