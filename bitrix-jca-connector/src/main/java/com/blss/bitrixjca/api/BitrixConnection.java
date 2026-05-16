package com.blss.bitrixjca.api;

import jakarta.resource.ResourceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BitrixConnection extends AutoCloseable {

    String createDocument(
            UUID orderId,
            String title,
            String body,
            BigDecimal totalAmount,
            String owner,
            String status,
            Instant createdAt,
            String deliveryPointName,
            String deliveryPointAddress,
            List<BitrixOrderItem> items
    ) throws ResourceException;

    String createCrmDeal(
            String title,
            String comments,
            BigDecimal amount,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException;

    Map<String, Object> callMethod(String method, Map<String, Object> payload) throws ResourceException;

    boolean isValid();

    void close() throws ResourceException;
}
