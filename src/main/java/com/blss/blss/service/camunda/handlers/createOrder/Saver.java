package com.blss.blss.service.camunda.handlers.createOrder;

import com.blss.blss.exception.InvalidOrderException;
import com.blss.blss.service.OrderService;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("createOrderSaverHandler")
@RequiredArgsConstructor
@ExternalTaskSubscription("create-order: save")
public class Saver implements ExternalTaskHandler {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            String owner = task.getVariable("owner");
            var location = CamundaHandlerSupport.uuid(task, "location");
            var productIds = CamundaHandlerSupport.uuidList(task, objectMapper, "productIds");
            var result = orderService.createOrder(owner, location, productIds);
            service.complete(task, Map.of(
                    "orderId", result.orderId().toString(),
                    "orderItemIds", objectMapper.writeValueAsString(result.positions()),
                    "processSuccess", true
            ));
        } catch (InvalidOrderException | IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
