package com.blss.orderservice.bitrix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class BitrixOrderDocumentClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final BitrixProperties properties;

    public BitrixOrderDocumentClient(
            WebClient.Builder webClientBuilder,
            BitrixProperties properties
    ) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getWebhookUrl());
    }

    public void createOrderDocument(BitrixOrderDocument document) {
        if (!isEnabled()) {
            log.info("Bitrix24 integration disabled, skipping document creation for order {}", document.orderId());
            return;
        }

        if (properties.getDocumentTemplateId() != null && properties.getDocumentTemplateId() > 0) {
            createBitrixGeneratedDocument(document);
            return;
        }

        if (properties.isUploadToCrmFallback()) {
            createCrmDeal(document);
            return;
        }

        log.info(
                "Bitrix24 enabled for order {}, but no template id is configured and CRM fallback is disabled",
                document.orderId()
        );
    }

    private void createBitrixGeneratedDocument(BitrixOrderDocument document) {
        Map<String, Object> payload = Map.of(
                "templateId", properties.getDocumentTemplateId(),
                "values", buildTemplateValues(document)
        );

        Map<String, Object> response = webClient.post()
                .uri(buildMethodUrl("documentgenerator.document.add.json"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .block();

        log.info("Bitrix24 generated document for order {}: {}", document.orderId(), summarizeResponse(response));
    }

    private void createCrmDeal(BitrixOrderDocument document) {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("TITLE", document.title());
        fields.put("COMMENTS", document.body());
        fields.put("OPPORTUNITY", document.totalAmount().toPlainString());
        if (properties.getAssignedById() != null && properties.getAssignedById() > 0) {
            fields.put("ASSIGNED_BY_ID", properties.getAssignedById());
        }
        if (properties.getCategoryId() != null && properties.getCategoryId() > 0) {
            fields.put("CATEGORY_ID", properties.getCategoryId());
        }

        Map<String, Object> payload = Map.of("fields", fields);

        String response = webClient.post()
                .uri(buildMethodUrl(properties.getDealMethod()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Bitrix24 CRM fallback accepted order document for {}: {}", document.orderId(), response);
    }

    private Map<String, Object> buildTemplateValues(BitrixOrderDocument document) {
        var values = new LinkedHashMap<String, Object>();
        values.put("DocumentTitle", document.title());
        values.put("DocumentBody", document.body());
        values.put("OrderId", document.orderId().toString());
        values.put("OrderNumber", "ORDER-" + document.orderId());
        values.put("OrderStatus", document.status());
        values.put("OrderOwner", document.owner());
        values.put("OrderCreatedAt", document.createdAt().toString());
        values.put("OrderTotalAmount", document.totalAmount().toPlainString());
        values.put("DeliveryPointName", document.deliveryPointName());
        values.put("DeliveryPointAddress", document.deliveryPointAddress());
        values.put("OrderItemsTable", buildItemsTable(document.items()));
        values.put("OrderItemsJson", buildItemsJson(document.items()));
        return values;
    }

    private String buildItemsTable(List<BitrixOrderDocumentItem> items) {
        List<String> lines = new ArrayList<>();
        lines.add("ID позиции | ID товара | Наименование | Цена | Ячейка");
        for (BitrixOrderDocumentItem item : items) {
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

    private List<Map<String, Object>> buildItemsJson(List<BitrixOrderDocumentItem> items) {
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

    private String summarizeResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "empty response";
        }
        Object result = response.get("result");
        return result != null ? result.toString() : response.toString();
    }

    private String buildMethodUrl(String method) {
        String normalizedBase = properties.getWebhookUrl().endsWith("/")
                ? properties.getWebhookUrl()
                : properties.getWebhookUrl() + "/";
        return normalizedBase + method;
    }
}
