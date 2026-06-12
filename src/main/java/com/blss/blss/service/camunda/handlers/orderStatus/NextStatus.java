package com.blss.blss.service.camunda.handlers.orderStatus;

import com.blss.blss.exception.InvalidActionException;
import com.blss.blss.service.OrderStatusUpdater;
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
@ExternalTaskSubscription("order-status: next")
public class NextStatus implements ExternalTaskHandler {

    private final OrderStatusUpdater orderStatusUpdater;
    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            log.info("Advancing order to next status: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            
            orderStatusUpdater.next(orderId);
            log.info("Order status advanced: processInstanceId={}, orderId={}, newStatus={}",
                    processInstanceId, orderId, orderService.getStatus(orderId).name());
            
            service.complete(task, Map.of(
                    "newStatus", orderService.getStatus(orderId).name(),
                    "processSuccess", true
            ));
        } catch (InvalidActionException | IllegalArgumentException e) {
            log.warn("Invalid next status action: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_STATUS_ACTION", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to advance order status: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
