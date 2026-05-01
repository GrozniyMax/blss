package com.blss.bitrixjca.api;

import jakarta.resource.ResourceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Connection handle for interacting with Bitrix24 REST API.
 * Provides methods for creating order documents and CRM deals.
 */
public interface BitrixConnection extends AutoCloseable {

    /**
     * Creates a document in Bitrix24 using the document generator.
     *
     * @param orderId           Order ID
     * @param title             Document title
     * @param body              Document body/content
     * @param totalAmount       Order total amount
     * @param owner             Order owner (username)
     * @param status            Order status
     * @param createdAt         Order creation timestamp
     * @param deliveryPointName Delivery point name
     * @param deliveryPointAddress Delivery point address
     * @param items             Order items
     * @return Document creation result (document ID or status)
     * @throws ResourceException if document creation fails
     */
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

    /**
     * Creates a CRM deal in Bitrix24 as a fallback option.
     *
     * @param title       Deal title
     * @param comments    Deal comments/description
     * @param amount      Deal amount
     * @param assignedById User ID to assign the deal to (optional)
     * @param categoryId  Category ID for the deal (optional)
     * @return Deal ID if successful
     * @throws ResourceException if deal creation fails
     */
    String createCrmDeal(
            String title,
            String comments,
            BigDecimal amount,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException;

    /**
     * Calls a Bitrix24 REST API method with the provided payload.
     *
     * @param method  REST API method name (e.g., "crm.deal.add.json")
     * @param payload Request payload
     * @return Response as a map
     * @throws ResourceException if the API call fails
     */
    Map<String, Object> callMethod(String method, Map<String, Object> payload) throws ResourceException;

    /**
     * Checks if the connection is valid.
     *
     * @return true if connection is valid
     */
    boolean isValid();

    /**
     * Closes the connection and releases resources.
     *
     * @throws ResourceException if closing fails
     */
    void close() throws ResourceException;
}
