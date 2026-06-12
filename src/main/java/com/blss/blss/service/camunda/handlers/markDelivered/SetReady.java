package com.blss.blss.service.camunda.handlers.markDelivered;

import com.blss.blss.domain.order.Status;
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
@ExternalTaskSubscription("mark-delivered: set-ready")
public class SetReady implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            log.info("Setting order ready for pickup: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            
            orderService.updateStatus(orderId, Status.READY_FOR_PICKUP);
            log.info("Order marked as ready for pickup: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            
            service.complete(task, Map.of(
                    "newStatus", Status.READY_FOR_PICKUP.name(),
                    "processSuccess", true
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order ID for setting ready: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to set order ready: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
