package com.blss.blss.service.camunda.handlers.pickup;

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
@ExternalTaskSubscription("order-pickup: reject")
public class RejectPickup implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            log.info("Rejecting order pickup: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            
            orderService.updateStatus(orderId, Status.CANCELED);
            log.info("Order pickup rejected: processInstanceId={}, orderId={}, status=Canceled",
                    processInstanceId, orderId);
            
            service.complete(task, Map.of("newStatus", Status.CANCELED.name(), "processSuccess", true));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order ID for pickup reject: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PICKUP", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to reject order pickup: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
