package com.blss.orderservice.client;

import com.blss.orderservice.client.dto.StatusTransactionPrepareRequest;
import com.blss.orderservice.domain.order.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatusServiceTransactionClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${status.service.url:http://localhost:25105}")
    private String statusServiceUrl;

    public void prepare(UUID txId, UUID orderId, Status status, String changedBy) {
        log.info("Preparing status-service transaction: txId={}, orderId={}, status={}", txId, orderId, status);

        webClientBuilder
                .baseUrl(statusServiceUrl)
                .build()
                .post()
                .uri("/internal/status-transactions/{txId}/prepare", txId)
                .bodyValue(new StatusTransactionPrepareRequest(orderId, status.name(), changedBy))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block(Duration.ofSeconds(6));
    }

    public void commit(UUID txId) {
        log.info("Committing status-service transaction: txId={}", txId);
        complete(txId, "commit");
    }

    public void rollback(UUID txId) {
        log.info("Rolling back status-service transaction: txId={}", txId);
        complete(txId, "rollback");
    }

    private void complete(UUID txId, String operation) {
        webClientBuilder
                .baseUrl(statusServiceUrl)
                .build()
                .post()
                .uri("/internal/status-transactions/{txId}/{operation}", txId, operation)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block(Duration.ofSeconds(6));
    }
}
