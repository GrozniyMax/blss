package com.blss.statusservice.service;

import com.blss.statusservice.client.UserServiceClient;
import com.blss.statusservice.db.OrderStatusHistoryRepo;
import com.blss.statusservice.domain.OrderStatusHistory;
import com.blss.statusservice.dto.output.OrderStatusHistoryResponse;
import com.blss.statusservice.dto.output.StatusHistoryEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String TX_PREPARED = "PREPARED";
    private static final String TX_CONFIRMED = "CONFIRMED";
    private static final String SYSTEM_USER = "SYSTEM";

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
                normalizeChangedBy(changedBy),
                null,
                TX_CONFIRMED
        );

        historyRepo.save(history);
        log.info("Status change saved: orderId={}", orderId);
    }

    @Transactional
    public void prepareStatusChange(UUID txId, UUID orderId, String status, String changedBy) {
        if (txId == null || orderId == null || status == null || status.isBlank()) {
            throw new IllegalArgumentException("txId, orderId and status are required");
        }

        OrderStatusHistory existing = historyRepo.findByTxId(txId);
        if (existing != null) {
            if (!TX_PREPARED.equals(existing.txState())) {
                throw new IllegalStateException("Status transaction is already completed: " + txId);
            }
            log.info("Status transaction is already prepared: txId={}", txId);
            return;
        }

        OrderStatusHistory history = new OrderStatusHistory(
                UUID.randomUUID(),
                orderId,
                status,
                Instant.now(),
                normalizeChangedBy(changedBy),
                txId,
                TX_PREPARED
        );

        historyRepo.save(history);
        log.info("Status transaction prepared: txId={}, orderId={}", txId, orderId);
    }

    @Transactional
    public void confirmPreparedStatusChange(UUID txId) {
        OrderStatusHistory prepared = getPreparedTransaction(txId);
        historyRepo.save(new OrderStatusHistory(
                prepared.id(),
                prepared.orderId(),
                prepared.status(),
                prepared.changedAt(),
                prepared.changedBy(),
                prepared.txId(),
                TX_CONFIRMED
        ));
        log.info("Status transaction confirmed: txId={}", txId);
    }

    @Transactional
    public void rollbackPreparedStatusChange(UUID txId) {
        OrderStatusHistory prepared = historyRepo.findByTxId(txId);
        if (prepared == null) {
            log.info("Status transaction is absent, rollback treated as completed: txId={}", txId);
            return;
        }
        if (!TX_PREPARED.equals(prepared.txState())) {
            log.info("Status transaction is not prepared, rollback skipped: txId={}, state={}", txId, prepared.txState());
            return;
        }

        historyRepo.deleteById(prepared.id());
        log.info("Status transaction rolled back: txId={}", txId);
    }

    private OrderStatusHistory getPreparedTransaction(UUID txId) {
        if (txId == null) {
            throw new IllegalArgumentException("txId is required");
        }

        OrderStatusHistory history = historyRepo.findByTxId(txId);
        if (history == null) {
            throw new IllegalArgumentException("Prepared status transaction not found: " + txId);
        }
        if (!TX_PREPARED.equals(history.txState())) {
            throw new IllegalStateException("Status transaction is not prepared: " + txId);
        }
        return history;
    }

    private String normalizeChangedBy(String changedBy) {
        return changedBy != null && !changedBy.isBlank() ? changedBy : SYSTEM_USER;
    }
}
