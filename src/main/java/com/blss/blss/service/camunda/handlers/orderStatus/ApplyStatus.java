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
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            String targetStatusValue = task.getVariable("targetStatus");

            if (targetStatusValue == null || targetStatusValue.isBlank()) {
                throw new InvalidActionException("Order status transition is not allowed by DMN decision table");
            }

            var targetStatus = Status.valueOf(targetStatusValue);
            orderService.updateStatus(orderId, targetStatus);
            log.info("Order status updated via DMN decision: orderId={}, targetStatus={}", orderId, targetStatus);

            service.complete(task, Map.of(
                    "newStatus", targetStatus.name(),
                    "processSuccess", true
            ));
        } catch (InvalidActionException | IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_STATUS_ACTION", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
