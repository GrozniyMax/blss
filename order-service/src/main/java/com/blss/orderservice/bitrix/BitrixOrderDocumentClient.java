package com.blss.orderservice.bitrix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class BitrixOrderDocumentClient {

    private final WebClient webClient;
    private final boolean enabled;
    private final String webhookUrl;
    private final String dealMethod;
    private final Long assignedById;
    private final Integer categoryId;

    public BitrixOrderDocumentClient(
            WebClient.Builder webClientBuilder,
            @Value("${bitrix.enabled:false}") boolean enabled,
            @Value("${bitrix.webhook-url:}") String webhookUrl,
            @Value("${bitrix.deal-method:crm.deal.add.json}") String dealMethod,
            @Value("${bitrix.assigned-by-id:0}") Long assignedById,
            @Value("${bitrix.category-id:0}") Integer categoryId
    ) {
        this.webClient = webClientBuilder.build();
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.dealMethod = dealMethod;
        this.assignedById = assignedById;
        this.categoryId = categoryId;
    }

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(webhookUrl);
    }

    public void createOrderDocument(BitrixOrderDocument document) {
        if (!isEnabled()) {
            log.info("Bitrix24 integration disabled, skipping document creation for order {}", document.orderId());
            return;
        }

        var fields = new LinkedHashMap<String, Object>();
        fields.put("TITLE", document.title());
        fields.put("COMMENTS", document.body());
        fields.put("OPPORTUNITY", document.totalAmount().toPlainString());
        if (assignedById != null && assignedById > 0) {
            fields.put("ASSIGNED_BY_ID", assignedById);
        }
        if (categoryId != null && categoryId > 0) {
            fields.put("CATEGORY_ID", categoryId);
        }

        Map<String, Object> payload = Map.of("fields", fields);

        webClient.post()
                .uri(buildMethodUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.info("Bitrix24 accepted order document for {}: {}", document.orderId(), response))
                .block();
    }

    private String buildMethodUrl() {
        var normalizedBase = webhookUrl.endsWith("/") ? webhookUrl : webhookUrl + "/";
        return normalizedBase + dealMethod;
    }
}
