package com.blss.orderservice.bitrix;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixConnectionFactory;
import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.exception.InvalidActionException;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.jms.OrderStatusProducer;
import com.blss.orderservice.service.order.OrderService;
import jakarta.resource.ResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitrixDealStatusSyncService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private final BitrixConnectionFactory connectionFactory;
    private final OrderService orderService;
    private final OrderStatusProducer orderStatusProducer;
    private final BitrixProperties bitrixProperties;

    public void syncOrderStatusFromDeal(String dealId) {
        if (dealId == null || dealId.isBlank()) {
            throw new InvalidActionException("Bitrix deal id is required");
        }

        Map<String, Object> deal = loadDeal(dealId.trim());
        UUID orderId = extractOrderId(deal)
                .orElseThrow(() -> new InvalidActionException("Cannot find order id in Bitrix deal " + dealId));
        Status status = extractStatus(deal)
                .orElseThrow(() -> new InvalidActionException("Unsupported Bitrix deal stage for deal " + dealId));

        Status current = orderService.getStatus(orderId);
        if (current == status) {
            log.info("Order status is already synced from Bitrix: orderId={}, status={}", orderId, status);
            return;
        }

        orderService.updateStatus(orderId, status);
        orderStatusProducer.sendStatusChange(orderId, status);
        log.info("Order status synced from Bitrix: dealId={}, orderId={}, {} -> {}", dealId, orderId, current, status);
    }

    public void syncOrderStatusesFromDeals() {
        int checked = 0;
        int updated = 0;

        for (Map<String, Object> deal : loadDeals()) {
            checked++;
            if (syncOrderStatusFromDeal(deal)) {
                updated++;
            }
        }

        log.info("Bitrix polling completed: checkedDeals={}, updatedOrders={}", checked, updated);
    }

    private boolean syncOrderStatusFromDeal(Map<String, Object> deal) {
        Object dealId = deal.get("ID");
        Optional<UUID> orderId = extractOrderId(deal);
        Optional<Status> status = extractStatus(deal);

        if (orderId.isEmpty() || status.isEmpty()) {
            log.debug("Skipping Bitrix deal without supported order title or stage: dealId={}", dealId);
            return false;
        }

        try {
            Status current = orderService.getStatus(orderId.get());
            if (current == status.get()) {
                return false;
            }

            orderService.updateStatus(orderId.get(), status.get());
            orderStatusProducer.sendStatusChange(orderId.get(), status.get());
            log.info("Order status synced from Bitrix polling: dealId={}, orderId={}, {} -> {}",
                    dealId, orderId.get(), current, status.get());
            return true;
        } catch (NotFoundException e) {
            log.warn("Skipping Bitrix deal for missing local order: dealId={}, orderId={}", dealId, orderId.get());
            return false;
        }
    }

    private List<Map<String, Object>> loadDeals() {
        try (BitrixConnection connection = connectionFactory.getConnection()) {
            Map<String, Object> filter = new LinkedHashMap<>();
            if (bitrixProperties.getCategoryId() != null && bitrixProperties.getCategoryId() > 0) {
                filter.put("CATEGORY_ID", bitrixProperties.getCategoryId());
            }

            List<Map<String, Object>> allDeals = new ArrayList<>();
            Object start = 0;
            while (start != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("order", Map.of("ID", "ASC"));
                payload.put("filter", filter);
                payload.put("select", List.of("ID", "TITLE", "STAGE_ID", "CATEGORY_ID"));
                payload.put("start", start);

                Map<String, Object> response = connection.callMethod("crm.deal.list.json", payload);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> deals = (List<Map<String, Object>>) response.get("result");
                if (deals != null) {
                    allDeals.addAll(deals);
                }

                start = response.get("next");
            }

            return allDeals;
        } catch (ResourceException e) {
            throw new InvalidActionException("Failed to load Bitrix deals: " + e.getMessage());
        }
    }

    private Map<String, Object> loadDeal(String dealId) {
        try (BitrixConnection connection = connectionFactory.getConnection()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> deal = (Map<String, Object>) connection
                    .callMethod("crm.deal.get.json", Map.of("id", dealId))
                    .get("result");

            if (deal == null) {
                throw new InvalidActionException("Bitrix deal not found: " + dealId);
            }
            return deal;
        } catch (ResourceException e) {
            throw new InvalidActionException("Failed to load Bitrix deal " + dealId + ": " + e.getMessage());
        }
    }

    private Optional<UUID> extractOrderId(Map<String, Object> deal) {
        return findUuid(deal.get("TITLE"));
    }

    private Optional<UUID> findUuid(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        var matcher = UUID_PATTERN.matcher(value.toString());
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(UUID.fromString(matcher.group()));
    }

    private Optional<Status> extractStatus(Map<String, Object> deal) {
        Object stage = deal.get("STAGE_ID");
        if (stage == null) {
            return Optional.empty();
        }

        return mapStageToStatus(stage.toString());
    }

    private Optional<Status> mapStageToStatus(String stageId) {
        String normalized = stageId.toUpperCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf(':');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(separatorIndex + 1);
        }

        return switch (normalized) {
            case "NEW" -> Optional.of(Status.CREATED);
            case "PREPARATION", "PREPAYMENT_INVOICE" -> Optional.of(Status.PROCESSING);
            case "EXECUTING" -> Optional.of(Status.IN_DELIVERY);
            case "FINAL_INVOICE" -> Optional.of(Status.READY_FOR_PICKUP);
            case "WON" -> Optional.of(Status.DONE);
            case "LOSE", "APOLOGY" -> Optional.of(Status.CANCELED);
            default -> Optional.empty();
        };
    }
}
