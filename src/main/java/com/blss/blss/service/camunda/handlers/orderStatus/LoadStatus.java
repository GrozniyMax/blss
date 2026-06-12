package com.blss.blss.service.camunda.handlers.orderStatus;

import com.blss.blss.service.OrderService;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("order-status: load")
public class LoadStatus implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            log.info("Loading order status: processInstanceId={}, orderId={}", processInstanceId, orderId);
            
            var status = orderService.getStatus(orderId);
            log.info("Order status loaded: processInstanceId={}, orderId={}, status={}",
                    processInstanceId, orderId, status);
            service.complete(task, Map.of("currentStatus", status.name()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order ID for status load: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_STATUS_ACTION", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to load order status: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
