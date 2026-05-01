package com.blss.bitrixjca.impl;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixOrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Managed connection implementation for Bitrix24 REST API.
 */
@Slf4j
public class BitrixManagedConnection implements ManagedConnection, BitrixConnection {

    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;

    private final BitrixManagedConnectionFactory managedConnectionFactory;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private PrintWriter logWriter;
    private final List<ConnectionEventListener> connectionEventListeners;
    private final List<BitrixConnection> connectionHandles;

    private boolean valid = true;

    public BitrixManagedConnection(BitrixManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.connectionEventListeners = new CopyOnWriteArrayList<>();
        this.connectionHandles = new CopyOnWriteArrayList<>();
        log.debug("Created new Bitrix24 managed connection");
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        log.debug("Creating connection handle for Bitrix24");
        BitrixConnectionImpl handle = new BitrixConnectionImpl(this);
        connectionHandles.add(handle);
        return handle;
    }

    @Override
    public void destroy() throws ResourceException {
        log.debug("Destroying Bitrix24 managed connection");
        valid = false;
        connectionHandles.clear();
        connectionEventListeners.clear();
    }

    @Override
    public void cleanup() throws ResourceException {
        log.debug("Cleaning up Bitrix24 managed connection");
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        for (ConnectionEventListener listener : connectionEventListeners) {
            listener.connectionClosed(event);
        }
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        log.debug("Associating connection handle with managed connection");
        if (connection instanceof BitrixConnectionImpl handle) {
            handle.associateWith(this);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        this.logWriter = printWriter;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new BitrixConnectionMetaData();
    }

    public BitrixManagedConnectionFactory getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    @Override
    public javax.transaction.xa.XAResource getXAResource() throws ResourceException {
        // This adapter does not support XA transactions
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        // This adapter does not support local transactions
        return null;
    }

    // ========== BitrixConnection Implementation ==========

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
        log.info("Creating Bitrix24 document for order {}", orderId);

        if (managedConnectionFactory.getDocumentTemplateId() != null && 
            managedConnectionFactory.getDocumentTemplateId() > 0) {
            return createBitrixGeneratedDocument(
                    orderId, title, body, totalAmount, owner, status, createdAt,
                    deliveryPointName, deliveryPointAddress, items
            );
        }

        if (Boolean.TRUE.equals(managedConnectionFactory.getUploadToCrmFallback())) {
            return createCrmDeal(title, body, totalAmount, 
                    managedConnectionFactory.getAssignedById(),
                    managedConnectionFactory.getCategoryId());
        }

        log.warn("Bitrix24 enabled for order {}, but no template ID configured and CRM fallback disabled", orderId);
        return null;
    }

    @Override
    public String createCrmDeal(
            String title,
            String comments,
            BigDecimal amount,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException {
        log.info("Creating Bitrix24 CRM deal: {}", title);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("TITLE", title);
        fields.put("COMMENTS", comments);
        fields.put("OPPORTUNITY", amount.toPlainString());
        
        if (assignedById != null && assignedById > 0) {
            fields.put("ASSIGNED_BY_ID", assignedById);
        }
        if (categoryId != null && categoryId > 0) {
            fields.put("CATEGORY_ID", categoryId);
        }

        Map<String, Object> payload = Map.of("fields", fields);

        String response = callMethod(managedConnectionFactory.getDealMethod(), payload)
                .toString();

        log.info("Bitrix24 CRM deal created: {}", response);
        return extractIdFromResponse(response);
    }

    @Override
    public Map<String, Object> callMethod(String method, Map<String, Object> payload) throws ResourceException {
        validateConnection();

        String url = buildMethodUrl(method);
        String jsonPayload;
        
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResourceException("Failed to serialize payload", e);
        }

        log.debug("Calling Bitrix24 method: {} with payload: {}", method, jsonPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(READ_TIMEOUT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(
                        response.body(), 
                        Map.class
                );
                log.debug("Bitrix24 response: {}", result);
                return result;
            } else {
                log.error("Bitrix24 API error: status={}, body={}", response.statusCode(), response.body());
                throw new ResourceException("Bitrix24 API error: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("Failed to call Bitrix24 API", e);
            throw new ResourceException("Failed to call Bitrix24 API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResourceException("Bitrix24 API call interrupted", e);
        }
    }

    @Override
    public boolean isValid() {
        return valid && managedConnectionFactory.getWebhookUrl() != null;
    }

    @Override
    public void close() throws ResourceException {
        log.debug("Closing Bitrix24 connection handle");
    }

    // ========== Private Methods ==========

    private String createBitrixGeneratedDocument(
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
        Map<String, Object> values = buildTemplateValues(
                orderId, title, body, totalAmount, owner, status, createdAt,
                deliveryPointName, deliveryPointAddress, items
        );

        Map<String, Object> payload = Map.of(
                "templateId", managedConnectionFactory.getDocumentTemplateId(),
                "values", values
        );

        Map<String, Object> response = callMethod("documentgenerator.document.add.json", payload);
        
        log.info("Bitrix24 generated document for order {}: {}", orderId, summarizeResponse(response));
        return extractIdFromResponse(response.toString());
    }

    private Map<String, Object> buildTemplateValues(
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
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("DocumentTitle", title);
        values.put("DocumentBody", body);
        values.put("OrderId", orderId.toString());
        values.put("OrderNumber", "ORDER-" + orderId);
        values.put("OrderStatus", status);
        values.put("OrderOwner", owner);
        values.put("OrderCreatedAt", createdAt.toString());
        values.put("OrderTotalAmount", totalAmount.toPlainString());
        values.put("DeliveryPointName", deliveryPointName);
        values.put("DeliveryPointAddress", deliveryPointAddress);
        values.put("OrderItemsTable", buildItemsTable(items));
        values.put("OrderItemsJson", buildItemsJson(items));
        return values;
    }

    private String buildItemsTable(List<BitrixOrderItem> items) {
        List<String> lines = new ArrayList<>();
        lines.add("ID позиции | ID товара | Наименование | Цена | Ячейка");
        for (BitrixOrderItem item : items) {
            lines.add(String.join(" | ",
                    item.itemId().toString(),
                    item.productId().toString(),
                    item.productName(),
                    item.price().toPlainString(),
                    Objects.toString(item.yacheyka(), "не назначена")
            ));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private List<Map<String, Object>> buildItemsJson(List<BitrixOrderItem> items) {
        return items.stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("itemId", item.itemId().toString());
                    map.put("productId", item.productId().toString());
                    map.put("productName", item.productName());
                    map.put("price", item.price().toPlainString());
                    map.put("yacheyka", Objects.toString(item.yacheyka(), ""));
                    return map;
                })
                .toList();
    }

    private String buildMethodUrl(String method) {
        String normalizedBase = managedConnectionFactory.getWebhookUrl().endsWith("/")
                ? managedConnectionFactory.getWebhookUrl()
                : managedConnectionFactory.getWebhookUrl() + "/";
        return normalizedBase + method;
    }

    private String summarizeResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "empty response";
        }
        Object result = response.get("result");
        return result != null ? result.toString() : response.toString();
    }

    private String extractIdFromResponse(String response) {
        if (response.contains("\"result\"")) {
            int start = response.indexOf("\"result\"") + 10;
            int end = response.indexOf(",", start);
            if (end == -1) end = response.indexOf("}", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return response;
    }

    private void validateConnection() throws ResourceException {
        if (!valid) {
            throw new ResourceException("Connection is not valid");
        }
        if (managedConnectionFactory.getWebhookUrl() == null) {
            throw new jakarta.resource.spi.SecurityException("Webhook URL is not configured");
        }
    }

    void notifyConnectionError(Exception e) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, e);
        for (ConnectionEventListener listener : connectionEventListeners) {
            listener.connectionErrorOccurred(event);
        }
    }

    // ========== Connection Metadata ==========

    private static class BitrixConnectionMetaData implements ManagedConnectionMetaData {

        @Override
        public String getEISProductName() throws ResourceException {
            return "Bitrix24 REST API";
        }

        @Override
        public String getEISProductVersion() throws ResourceException {
            return "1.0";
        }

        @Override
        public int getMaxConnections() throws ResourceException {
            return 0;
        }

        @Override
        public String getUserName() throws ResourceException {
            return null;
        }
    }
}
