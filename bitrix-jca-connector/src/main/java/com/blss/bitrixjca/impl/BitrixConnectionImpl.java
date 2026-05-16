package com.blss.bitrixjca.impl;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixOrderItem;
import jakarta.resource.ResourceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BitrixConnectionImpl implements BitrixConnection {

    private BitrixManagedConnection managedConnection;

    public BitrixConnectionImpl(BitrixManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    void associateWith(BitrixManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    @Override
    public String createDocument(
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
    ) throws ResourceException {
        return managedConnection.createDocument(
                orderId, title, body, totalAmount, owner, status, createdAt,
                deliveryPointName, deliveryPointAddress, items
        );
    }

    @Override
    public String createCrmDeal(
            String title,
            String comments,
            BigDecimal amount,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException {
        return managedConnection.createCrmDeal(title, comments, amount, assignedById, categoryId);
    }

    @Override
    public Map<String, Object> callMethod(String method, Map<String, Object> payload) throws ResourceException {
        return managedConnection.callMethod(method, payload);
    }

    @Override
    public boolean isValid() {
        return managedConnection != null && managedConnection.isValid();
    }

    @Override
    public void close() throws ResourceException {
        if (managedConnection != null) {
            managedConnection.close();
        }
    }
}
