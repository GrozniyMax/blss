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

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("order-pickup: verify")
public class VerifyPickup implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            var status = orderService.getStatus(orderId);
            if (status != Status.READY_FOR_PICKUP) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_PICKUP",
                        "Order is not ready for pickup: " + status);
                return;
            }
            service.complete(task);
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PICKUP", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
