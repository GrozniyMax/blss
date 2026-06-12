package com.blss.blss.service.camunda.handlers.markDelivered;

import com.blss.blss.db.order.OrderItemRepo;
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
@ExternalTaskSubscription("mark-delivered: check-ready")
public class CheckReady implements ExternalTaskHandler {

    private final OrderService orderService;
    private final OrderItemRepo orderItemRepo;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            log.info("Checking order readiness for delivery: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            
            boolean ready = orderService.getStatus(orderId) == Status.IN_DELIVERY
                    && orderItemRepo.countItemsWithoutYacheyka(orderId) == 0;
            
            log.info("Order readiness check: processInstanceId={}, orderId={}, ready={}",
                    processInstanceId, orderId, ready);
            service.complete(task, Map.of("orderReady", ready, "processSuccess", true));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order ID for delivery check: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK", e.getMessage());
        } catch (Exception e) {
            log.error("Delivery readiness check failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
