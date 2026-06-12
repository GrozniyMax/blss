package com.blss.blss.service.camunda.handlers.orderStatus;

import com.blss.blss.domain.order.Status;
import com.blss.blss.exception.InvalidActionException;
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
@ExternalTaskSubscription("order-status: apply")
public class ApplyStatus implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            String targetStatusValue = task.getVariable("targetStatus");

            log.info("Applying order status via DMN: processInstanceId={}, orderId={}, targetStatus={}",
                    processInstanceId, orderId, targetStatusValue);

            if (targetStatusValue == null || targetStatusValue.isBlank()) {
                throw new InvalidActionException("Order status transition is not allowed by DMN decision table");
            }

            var targetStatus = Status.valueOf(targetStatusValue);
            orderService.updateStatus(orderId, targetStatus);
            log.info("Order status updated via DMN decision: processInstanceId={}, orderId={}, targetStatus={}",
                    processInstanceId, orderId, targetStatus);

            service.complete(task, Map.of(
                    "newStatus", targetStatus.name(),
                    "processSuccess", true
            ));
        } catch (InvalidActionException | IllegalArgumentException e) {
            log.warn("Invalid status action: processInstanceId={}, error={}", processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_STATUS_ACTION", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to apply order status: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
