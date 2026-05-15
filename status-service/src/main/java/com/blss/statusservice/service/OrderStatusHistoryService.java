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

    private static final String TX_STATE_PREPARED = "PREPARED";
    private static final String TX_STATE_CONFIRMED = "CONFIRMED";

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

    public void saveStatusChange(UUID orderId, String status) {
        log.info("Saving status change: orderId={}, status={}", orderId, status);

        OrderStatusHistory history = new OrderStatusHistory(
                orderId,
                status,
                Instant.now()
        );

        historyRepo.save(history);
        log.info("Status change saved: orderId={}", orderId);
    }

    public void prepareStatusChange(UUID txId, UUID orderId, String status, String changedBy) {
        log.info("Preparing status change: txId={}, orderId={}, status={}, changedBy={}",
                txId, orderId, status, changedBy);

        var existing = historyRepo.findByTxId(txId);
        if (existing.isPresent()) {
            log.info("Status change already prepared or confirmed: txId={}, state={}", txId, existing.get().txState());
            return;
        }

        OrderStatusHistory history = new OrderStatusHistory(
                orderId,
                status,
                Instant.now(),
                txId,
                TX_STATE_PREPARED
        );

        historyRepo.save(history);
        log.info("Status change prepared: txId={}, orderId={}", txId, orderId);
    }

    public void confirmPreparedStatusChange(UUID txId) {
        log.info("Confirming prepared status change: txId={}", txId);

        int updated = historyRepo.confirmPreparedByTxId(txId);
        if (updated == 0) {
            historyRepo.findByTxId(txId)
                    .filter(history -> TX_STATE_CONFIRMED.equals(history.txState()))
                    .orElseThrow(() -> new IllegalArgumentException("Prepared status transaction not found: " + txId));
        }

        log.info("Prepared status change confirmed: txId={}", txId);
    }

    public void rollbackPreparedStatusChange(UUID txId) {
        log.info("Rolling back prepared status change: txId={}", txId);

        int deleted = historyRepo.deletePreparedByTxId(txId);
        if (deleted == 0) {
            historyRepo.findByTxId(txId)
                    .filter(history -> TX_STATE_CONFIRMED.equals(history.txState()))
                    .ifPresent(history -> {
                        throw new IllegalStateException("Cannot rollback confirmed status transaction: " + txId);
                    });
        }

        log.info("Prepared status change rolled back: txId={}", txId);
    }
}
