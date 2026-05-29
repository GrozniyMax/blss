package com.blss.orderservice.service.tx;

import com.blss.orderservice.client.StatusServiceTransactionClient;
import com.blss.orderservice.domain.order.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedStatusTransactionCoordinator {

    private final StatusServiceTransactionClient statusServiceTransactionClient;

    public void prepareStatusChange(UUID txId, UUID orderId, Status status, String changedBy) {
        registerCompletion(txId);
        statusServiceTransactionClient.prepare(txId, orderId, status, changedBy);
    }

    private void registerCompletion(UUID txId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Distributed status transaction must be started inside an active transaction");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                statusServiceTransactionClient.commit(txId);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        statusServiceTransactionClient.rollback(txId);
                    } catch (RuntimeException ex) {
                        log.error("Failed to rollback status-service transaction: txId={}", txId, ex);
                    }
                }
            }
        });
    }
}
