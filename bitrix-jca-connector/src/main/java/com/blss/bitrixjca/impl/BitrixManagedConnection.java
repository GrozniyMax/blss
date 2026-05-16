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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Управляемое соединение с ебучи Bitrix24.
 */
@Slf4j
public class BitrixManagedConnection implements ManagedConnection, BitrixConnection {

    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    private static final DateTimeFormatter DOCUMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
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
        log.info("Creating Bitrix24 document for order {}", orderId);

        if (managedConnectionFactory.getDocumentTemplateId() != null && 
            managedConnectionFactory.getDocumentTemplateId() > 0) {
            return createBitrixGeneratedDocument(
                    orderId, title, body, totalAmount, owner, status, createdAt,
                    deliveryPointName, deliveryPointAddress, items
            );
        }

        if (Boolean.TRUE.equals(managedConnectionFactory.getUploadToCrmFallback())) {
            return syncCrmDeal(orderId, title, body, totalAmount, status,
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

    private String syncCrmDeal(
            UUID orderId,
            String title,
            String comments,
            BigDecimal amount,
            String status,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException {
        String stageId = resolveDealStageId(status, categoryId);
        Optional<String> existingDealId = findDealIdByTitle(title);

        if (existingDealId.isPresent()) {
            updateCrmDeal(existingDealId.get(), title, comments, amount, stageId, assignedById, categoryId);
            log.info("Bitrix24 CRM deal updated for order {}: dealId={}, stageId={}", orderId, existingDealId.get(), stageId);
            return existingDealId.get();
        }

        return createCrmDeal(title, comments, amount, stageId, assignedById, categoryId);
    }

    private String createCrmDeal(
            String title,
            String comments,
            BigDecimal amount,
            String stageId,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException {
        log.info("Creating Bitrix24 CRM deal: {}, stageId={}", title, stageId);

        Map<String, Object> fields = buildDealFields(title, comments, amount, stageId, assignedById, categoryId);
        Map<String, Object> response = callMethod(managedConnectionFactory.getDealMethod(), Map.of("fields", fields));
        String dealId = extractResultId(response);

        log.info("Bitrix24 CRM deal created: dealId={}, response={}", dealId, response);
        return dealId;
    }

    private void updateCrmDeal(
            String dealId,
            String title,
            String comments,
            BigDecimal amount,
            String stageId,
            Long assignedById,
            Integer categoryId
    ) throws ResourceException {
        Map<String, Object> payload = Map.of(
                "id", dealId,
                "fields", buildDealFields(title, comments, amount, stageId, assignedById, categoryId)
        );

        callMethod("crm.deal.update.json", payload);
    }

    private Optional<String> findDealIdByTitle(String title) throws ResourceException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("filter", Map.of("TITLE", title));
        payload.put("select", List.of("ID"));
        payload.put("order", Map.of("ID", "DESC"));

        Map<String, Object> response = callMethod("crm.deal.list.json", payload);
        Object result = response.get("result");
        if (!(result instanceof List<?> deals) || deals.isEmpty()) {
            return Optional.empty();
        }

        Object firstDeal = deals.get(0);
        if (!(firstDeal instanceof Map<?, ?> deal)) {
            return Optional.empty();
        }

        Object id = deal.get("ID");
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }

    private Map<String, Object> buildDealFields(
            String title,
            String comments,
            BigDecimal amount,
            String stageId,
            Long assignedById,
            Integer categoryId
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("TITLE", title);
        fields.put("COMMENTS", comments);
        fields.put("OPPORTUNITY", amount != null ? amount.toPlainString() : BigDecimal.ZERO.toPlainString());
        fields.put("STAGE_ID", stageId);

        if (assignedById != null && assignedById > 0) {
            fields.put("ASSIGNED_BY_ID", assignedById);
        }
        if (categoryId != null && categoryId > 0) {
            fields.put("CATEGORY_ID", categoryId);
        }
        return fields;
    }

    private String resolveDealStageId(String status, Integer categoryId) {
        String stage = switch (defaultString(status)) {
            case "CREATED" -> "NEW";
            case "PROCESSING" -> "PREPARATION";
            case "IN_DELIVERY" -> "EXECUTING";
            case "READY_FOR_PICKUP" -> "FINAL_INVOICE";
            case "DONE" -> "WON";
            case "CANCELED" -> "LOSE";
            default -> "NEW";
        };

        if (categoryId == null || categoryId <= 0) {
            return stage;
        }
        return "C" + categoryId + ":" + stage;
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
        log.info("Outgoing Bitrix24 request: method={}, url={}, body={}", method, url, jsonPayload);

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
        log.info(
                "Bitrix document payload format: templateId={}, valuesType={}, productsCount={}",
                managedConnectionFactory.getDocumentTemplateId(),
                values.getClass().getSimpleName(),
                values.get("Products") instanceof List<?> products ? products.size() : 0
        );
        try {
            log.debug("Bitrix document payload JSON: {}", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Bitrix document payload for debug logging: {}", e.getMessage());
        }

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
        String documentNumber = "ACT-" + orderId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String documentDate = DOCUMENT_DATE_FORMAT.format(createdAt.atZone(ZoneId.systemDefault()));
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BitrixOrderItem firstItem = items != null && !items.isEmpty() ? items.get(0) : null;
        BigDecimal firstPrice = firstItem != null && firstItem.price() != null ? firstItem.price() : BigDecimal.ZERO;
        int productsIndex = 1;
        String productsProductName = firstItem != null ? defaultString(firstItem.productName()) : "";
        int productsProductQuantity = 1;
        String productsProductMeasureName = defaultString(managedConnectionFactory.getDefaultProductMeasureName());
        BigDecimal productsProductPriceRaw = firstPrice;
        BigDecimal productsProductPriceRawSum = firstPrice;

        String clientName = defaultString(owner);
        String clientPhone = defaultString(managedConnectionFactory.getDefaultClientPhone());
        String requisiteRegisteredAddressText = defaultString(deliveryPointAddress);
        String taxesTaxTitle = defaultString(managedConnectionFactory.getDefaultTaxTitle());
        String taxesTaxRate = defaultString(managedConnectionFactory.getDefaultTaxRate());

        String myCompanyRequisiteRqCompanyName = defaultString(managedConnectionFactory.getMyCompanyName());
        String myCompanyRequisiteRqInn = defaultString(managedConnectionFactory.getMyCompanyInn());
        String myCompanyRequisiteRqKpp = defaultString(managedConnectionFactory.getMyCompanyKpp());
        String myCompanyRequisiteRegisteredAddressText = defaultString(managedConnectionFactory.getMyCompanyAddress());
        String myCompanyPhone = defaultString(managedConnectionFactory.getMyCompanyPhone());
        String myCompanyBankDetailRqBankName = defaultString(managedConnectionFactory.getMyCompanyBankName());
        String myCompanyBankDetailRqBik = defaultString(managedConnectionFactory.getMyCompanyBik());
        String myCompanyBankDetailRqAccNum = defaultString(managedConnectionFactory.getMyCompanyAccNum());
        String myCompanyBankDetailRqCorAccNum = defaultString(managedConnectionFactory.getMyCompanyCorAccNum());
        String myCompanyRequisiteRqDirector = defaultString(managedConnectionFactory.getMyCompanyDirector());

        String documentTitle = defaultString(title);
        String documentBody = defaultString(body);
        String orderStatus = defaultString(status);
        String orderOwner = defaultString(owner);
        String orderCreatedAt = createdAt.toString();
        String orderTotalAmount = total.toPlainString();
        String deliveryPointNameValue = defaultString(deliveryPointName);
        String deliveryPointAddressValue = defaultString(deliveryPointAddress);
        String totalRaw = total.toPlainString();
        String totalSum = total.toPlainString();
        List<Map<String, Object>> productsItems = buildProducts(items);

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("DocumentNumber", documentNumber);
        values.put("DocumentCreateTime", documentDate);
        values.put("ClientName", clientName);
        values.put("ProductsIndex", productsIndex);
        values.put("ProductsProductName", productsProductName);
        values.put("ProductsProductQuantity", productsProductQuantity);
        values.put("ProductsProductMeasureName", productsProductMeasureName);
        values.put("ProductsProductPriceRaw", productsProductPriceRaw);
        values.put("ProductsProductPriceRawSum", productsProductPriceRawSum);
        values.put("Products", productsItems);
        values.put("PRODUCTS", productsItems);
        values.put("ClientPhone", clientPhone);
        values.put("RequisiteRegisteredAddressText", requisiteRegisteredAddressText);
        values.put("RequisiteRqInn", "");
        values.put("RequisiteRqKpp", "");
        values.put("BankDetailRqBankName", "");
        values.put("BankDetailRqBik", "");
        values.put("BankDetailRqAccNum", "");
        values.put("BankDetailRqCorAccNum", "");

        values.put("MyCompanyRequisiteRqCompanyName", myCompanyRequisiteRqCompanyName);
        values.put("MyCompanyRequisiteRqInn", myCompanyRequisiteRqInn);
        values.put("MyCompanyRequisiteRqKpp", myCompanyRequisiteRqKpp);
        values.put("MyCompanyRequisiteRegisteredAddressText", myCompanyRequisiteRegisteredAddressText);
        values.put("MyCompanyPhone", myCompanyPhone);
        values.put("MyCompanyBankDetailRqBankName", myCompanyBankDetailRqBankName);
        values.put("MyCompanyBankDetailRqBik", myCompanyBankDetailRqBik);
        values.put("MyCompanyBankDetailRqAccNum", myCompanyBankDetailRqAccNum);
        values.put("MyCompanyBankDetailRqCorAccNum", myCompanyBankDetailRqCorAccNum);
        values.put("MyCompanyRequisiteRqDirector", myCompanyRequisiteRqDirector);

        values.put("TotalRaw", totalRaw);
        values.put("TotalSum", totalSum);
        values.put("TaxesTaxTitle", taxesTaxTitle);
        values.put("TaxesTaxRate", taxesTaxRate);
        values.put("TaxesTaxValue", "0");

        values.put("DocumentTitle", documentTitle);
        values.put("DocumentBody", documentBody);
        values.put("OrderId", orderId.toString());
        values.put("OrderStatus", orderStatus);
        values.put("OrderOwner", orderOwner);
        values.put("OrderCreatedAt", orderCreatedAt);
        values.put("OrderTotalAmount", orderTotalAmount);
        values.put("DeliveryPointName", deliveryPointNameValue);
        values.put("DeliveryPointAddress", deliveryPointAddressValue);
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

    private List<Map<String, Object>> buildProducts(List<BitrixOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        BitrixOrderItem item = items.get(0);
        BigDecimal price = item.price() != null ? item.price() : BigDecimal.ZERO;
        int index = 1;
        String productName = defaultString(item.productName());
        int productQuantity = 1;
        String productMeasureName = defaultString(managedConnectionFactory.getDefaultProductMeasureName());
        BigDecimal productPriceRaw = price;
        BigDecimal productPriceRawSum = price;

        int productsIndex = 1;
        String productsProductName = defaultString(item.productName());
        int productsProductQuantity = 1;
        String productsProductMeasureName = defaultString(managedConnectionFactory.getDefaultProductMeasureName());
        BigDecimal productsProductPriceRaw = price;
        BigDecimal productsProductPriceRawSum = price;

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("NAME", productName);
        product.put("MEASURE_NAME", productMeasureName);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("INDEX", "index");
        map.put("PRODUCT", "product");
        map.put("QUANTITY", productQuantity);
        map.put("PRICE_RAW", productPriceRaw);
        map.put("PRICE_RAW_SUM", productPriceRawSum);

        map.put("Index", "index");
        map.put("ProductName", "productName");
        map.put("ProductQuantity", productQuantity);
        map.put("ProductMeasureName", productMeasureName);
        map.put("ProductPriceRaw", productPriceRaw);
        map.put("ProductPriceRawSum", productPriceRawSum);
        map.put("ProductsIndex", productsIndex);
        map.put("ProductsProductName", productsProductName);
        map.put("ProductsProductQuantity", productsProductQuantity);
        map.put("ProductsProductMeasureName", productsProductMeasureName);
        map.put("ProductsProductPriceRaw", productsProductPriceRaw);
        map.put("ProductsProductPriceRawSum", productsProductPriceRawSum);
        return List.of(map);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
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

    private String extractResultId(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object result = response.get("result");
        return result == null ? response.toString() : result.toString();
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
