package com.blss.blss.camunda;

import com.blss.blss.domain.Product;
import com.blss.blss.domain.order.Status;
import com.blss.blss.exception.InvalidActionException;
import com.blss.blss.service.OrderService;
import com.blss.blss.service.StoreService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CamundaExternalTaskWorkers {

    private static final long RETRY_TIMEOUT_MILLIS = 30_000L;

    ExternalTaskClient externalTaskClient;
    StoreService storeService;
    OrderService orderService;
    ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() {
        subscribe("product.create", this::createProduct);
        subscribe("order.create", this::createOrder);
        subscribe("order.pickup.verify", this::verifyPickup);
        subscribe("order.pickup.complete", this::completePickup);
        subscribe("order.pickup.reject", this::rejectPickup);
    }

    private void subscribe(String topic, ExternalTaskHandler handler) {
        externalTaskClient.subscribe(topic)
                .lockDuration(10_000)
                .handler((task, service) -> {
                    try {
                        handler.execute(task, service);
                    } catch (Exception e) {
                        fail(task, service, e);
                    }
                })
                .open();
    }

    private void createProduct(ExternalTask task, ExternalTaskService service) {
        var productId = storeService.createProduct(
                new Product(null, task.getVariable("name"), new BigDecimal(task.getVariable("price").toString())),
                task.getVariable("initialCount")
        );

        service.complete(task, Variables.createVariables()
                .putValue("productId", productId.toString()));
    }

    private void createOrder(ExternalTask task, ExternalTaskService service) {
        var creation = orderService.createOrder(
                task.getVariable("owner"),
                UUID.fromString(task.getVariable("location")),
                parseProductIds(task.getVariable("productIds"))
        );

        service.complete(task, Variables.createVariables()
                .putValue("orderId", creation.orderId().toString())
                .putValue("positionIds", creation.positions().stream().map(UUID::toString).toList()));
    }

    private void verifyPickup(ExternalTask task, ExternalTaskService service) {
        var orderId = UUID.fromString(task.getVariable("orderId"));
        var status = orderService.getStatus(orderId);
        if (status != Status.READY_FOR_PICKUP) {
            throw new InvalidActionException("Order must be READY_FOR_PICKUP before pickup");
        }

        service.complete(task, Variables.createVariables()
                .putValue("orderStatus", status.name()));
    }

    private void completePickup(ExternalTask task, ExternalTaskService service) {
        orderService.updateStatus(UUID.fromString(task.getVariable("orderId")), Status.DONE);
        service.complete(task);
    }

    private void rejectPickup(ExternalTask task, ExternalTaskService service) {
        orderService.updateStatus(UUID.fromString(task.getVariable("orderId")), Status.CANCELED);
        service.complete(task);
    }

    private List<UUID> parseProductIds(Object rawValue) {
        if (rawValue instanceof Collection<?> values) {
            return values.stream()
                    .map(Object::toString)
                    .map(UUID::fromString)
                    .toList();
        }
        return objectMapper.convertValue(rawValue, new TypeReference<List<String>>() {
                }).stream()
                .map(UUID::fromString)
                .toList();
    }

    private void fail(ExternalTask task, ExternalTaskService service, Exception e) {
        var retries = task.getRetries() == null ? 2 : Math.max(task.getRetries() - 1, 0);
        service.handleFailure(task, e.getMessage(), exceptionDetails(e), retries, RETRY_TIMEOUT_MILLIS);
    }

    private String exceptionDetails(Exception e) {
        var builder = new StringBuilder(e.toString());
        for (StackTraceElement element : e.getStackTrace()) {
            builder.append(System.lineSeparator()).append("\tat ").append(element);
        }
        return builder.toString();
    }
}
