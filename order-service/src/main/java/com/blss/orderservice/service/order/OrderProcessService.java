package com.blss.orderservice.service.order;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderProcessService {

    private static final String CREATE_ORDER_PROCESS = "order-create-process";
    private static final String ADVANCE_ORDER_STATUS_PROCESS = "order-status-advance-process";
    private static final String CANCEL_ORDER_PROCESS = "order-cancel-process";
    private static final String REVERT_ORDER_STATUS_PROCESS = "order-status-revert-process";
    private static final String ITEM_DELIVERED_PROCESS = "order-item-delivered-process";
    private static final String DELIVERY_READINESS_PROCESS = "order-delivery-readiness-process";

    RuntimeService runtimeService;

    public OrderService.CreationOrderResponse createOrder(String owner, UUID location, List<UUID> productIds) {
        var process = runtimeService.createProcessInstanceByKey(CREATE_ORDER_PROCESS)
                .setVariables(Map.of(
                "owner", owner,
                "location", location.toString(),
                "productIds", productIds.stream().map(UUID::toString).toList()
                ))
                .executeWithVariablesInReturn();

        var variables = process.getVariables();
        var orderId = UUID.fromString((String) variables.get("orderId"));

        @SuppressWarnings("unchecked")
        var positionIds = ((List<String>) variables.get("positionIds")).stream()
                .map(UUID::fromString)
                .toList();

        return new OrderService.CreationOrderResponse(orderId, positionIds);
    }

    public void advanceStatus(UUID orderId) {
        startOrderProcess(ADVANCE_ORDER_STATUS_PROCESS, orderId);
    }

    public void cancel(UUID orderId) {
        startOrderProcess(CANCEL_ORDER_PROCESS, orderId);
    }

    public void revertStatus(UUID orderId, String revertFrom) {
        runtimeService.startProcessInstanceByKey(REVERT_ORDER_STATUS_PROCESS, Map.of(
                "orderId", orderId.toString(),
                "revertFrom", revertFrom
        ));
    }

    public void markDelivered(UUID itemId, String yacheyka) {
        runtimeService.startProcessInstanceByKey(ITEM_DELIVERED_PROCESS, Map.of(
                "itemId", itemId.toString(),
                "yacheyka", yacheyka
        ));
    }

    public void checkDeliveryReadiness(UUID orderId) {
        startOrderProcess(DELIVERY_READINESS_PROCESS, orderId);
    }

    private void startOrderProcess(String processKey, UUID orderId) {
        runtimeService.startProcessInstanceByKey(processKey, Map.of("orderId", orderId.toString()));
    }
}
